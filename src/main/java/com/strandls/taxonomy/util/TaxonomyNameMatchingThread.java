package com.strandls.taxonomy.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strandls.esmodule.controllers.EsServicesApi;
import com.strandls.esmodule.pojo.TaxonMatchQuery;
import com.strandls.utility.controller.UtilityServiceApi;
import com.strandls.utility.pojo.ParsedName;

/**
 * Reads the uploaded workbook, matches every name in it, and writes a COPY of
 * the workbook with the match details appended as extra columns on the first
 * sheet. The uploaded file itself is never modified.
 *
 * The result columns are put at the FRONT of the sheet (columns A, B, ...) and
 * the original columns are moved right after them. The settings used (index,
 * synonym, cname, rank) are saved in a hidden sheet named {@value #META_SHEET},
 * so {@link NameMatchingResultReader} can rebuild the results from the file
 * alone.
 *
 * Result columns, in order:
 * <ul>
 * <li>Match Status, Match Count, Matched Name, Matched Rank, Matched Status,
 * Position, Group, Hierarchy, Taxon Id, Accepted Id, Species Id</li>
 * <li>Synonym Matches (only if a synonym column was given)</li>
 * <li>Common Name Matches, Unmatched Common Names (only if a common-name column
 * was given)</li>
 * <li>"&lt;Rank&gt; Match" for every rank column given in {@code rank}</li>
 * </ul>
 * When a name has several matches, each per-match column lists the values in
 * the same order, separated by " | ", so the n-th value of every column belongs
 * to the same match.
 */
public class TaxonomyNameMatchingThread implements Runnable {

	/**
	 * The uploaded file, read into memory while the request is still open. Jersey
	 * deletes the multipart data once the request ends, so the background thread
	 * must never read from the FormDataBodyPart itself.
	 */
	private byte[] fileBytes;
	private String uploadedFileName;
	private Integer index;
	private Integer synonym;
	private Integer cname;
	private String rank;
	private EsServicesApi esServicesApi;
	private UtilityServiceApi utilityServiceApi;
	private String basePath;
	private String modulePath;

	/** Where the result copy was written; null until run() finishes successfully. */
	private volatile File outputFile;

	private static final int NAME_MATCH_BATCH_SIZE = 1000;
	private static final int NAME_PARSE_BATCH_SIZE = 1000;

	/** Excel's hard limit on characters in a single cell. */
	private static final int MAX_CELL_LENGTH = 32767;

	private static final String MATCH_SEPARATOR = " | ";
	private static final String MISSING = "-";

	/**
	 * Hidden sheet holding the settings used to process the file, so the
	 * processed file can be read back without any other input.
	 */
	public static final String META_SHEET = "name_matching_meta";
	public static final String META_VERSION = "version";
	public static final String META_INDEX = "index";
	public static final String META_SYNONYM = "synonym";
	public static final String META_CNAME = "cname";
	public static final String META_RANK = "rank";
	public static final String META_RESULT_COLUMNS = "resultColumns";
	public static final String META_VERSION_VALUE = "1";

	/**
	 * Hidden sheet with the full details (as JSON) of every synonym and rank-column
	 * match, which the readable result columns only summarise.
	 */
	public static final String DETAILS_SHEET = "name_matching_details";
	public static final String DETAIL_SYNONYM = "synonym";
	public static final String DETAIL_RANK = "rank";
	/** JSON is split into pieces of this size, safely under Excel's 32767-char cell limit. */
	private static final int DETAIL_CHUNK = 30000;
	private static final ObjectMapper JSON = new ObjectMapper();

	private Sheet detailsSheet;
	private int detailsRowNum;

	/**
	 * How far the original columns were shifted right to make room for the result
	 * columns at the front. Original column N is at N + colOffset in the copy.
	 */
	private int colOffset = 0;

	private static final String[] MAIN_RESULT_COLUMNS = { "Match Status", "Match Count", "Matched Name",
			"Matched Rank", "Matched Status", "Position", "Group", "Hierarchy", "Taxon Id", "Accepted Id",
			"Species Id" };

	private final Logger logger = LoggerFactory.getLogger(TaxonomyNameMatchingThread.class);

	// Lookup results shared by the passes in run()
	private Map<String, ParsedName> parsedNames = new HashMap<>();
	private Map<String, List<Object>> matchResults = new HashMap<>();
	private Map<Long, Long> speciesIds = new HashMap<>();
	private Map<String, String> hierFirstCell = new HashMap<>();
	private Map<Integer, String> rankMap = new HashMap<>();

	/**
	 * Writes the result copy to the system temp directory. Prefer the constructor
	 * that takes basePath / modulePath.
	 */
	public TaxonomyNameMatchingThread(FormDataBodyPart filePart, Integer index, Integer synonym, Integer cname,
			String rank, EsServicesApi esServicesApi, UtilityServiceApi utilityServiceApi) {
		this(filePart, index, synonym, cname, rank, esServicesApi, utilityServiceApi, null, null);
	}

	/**
	 * The result copy is written to
	 * {@code basePath + modulePath + File.separator + fileName}, the same way the
	 * species list download builds its path. Missing folders are created.
	 *
	 * @param basePath   storage base path (as used for downloads)
	 * @param modulePath module folder under basePath, e.g. "/taxonomy/namematching"
	 */
	public TaxonomyNameMatchingThread(FormDataBodyPart filePart, Integer index, Integer synonym, Integer cname,
			String rank, EsServicesApi esServicesApi, UtilityServiceApi utilityServiceApi, String basePath,
			String modulePath) {
		super();
		// Read now (request thread), not in run() (background thread)
		try (InputStream in = filePart.getValueAs(InputStream.class)) {
			this.fileBytes = in.readAllBytes();
		} catch (IOException e) {
			throw new UncheckedIOException("Could not read uploaded file", e);
		}
		try {
			if (filePart.getContentDisposition() != null) {
				this.uploadedFileName = filePart.getContentDisposition().getFileName();
			}
		} catch (Exception e) {
			logger.debug("Could not read uploaded file name: {}", e.getMessage());
		}
		this.index = index;
		this.synonym = synonym;
		this.cname = cname;
		this.rank = rank;
		this.esServicesApi = esServicesApi;
		this.utilityServiceApi = utilityServiceApi;
		this.basePath = basePath;
		this.modulePath = modulePath;
	}

	/** The written copy, or null if the thread has not finished or failed. */
	public File getOutputFile() {
		return outputFile;
	}

	@Override
	public void run() {

		// ---- Work on a copy of the incoming file (bytes read in the constructor) ----
		try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileBytes))) {
			Sheet sheet = workbook.getSheetAt(0);

			if (rank != null && !rank.isBlank()) {
				for (String ranki : rank.split("\\|")) {
					String[] rankDet = ranki.split("\\s*:\\s*");
					rankMap.put(Integer.valueOf(rankDet[1].trim()), rankDet[0].trim());
				}
			}

			Row headersRow = sheet.getRow(0);
			if (headersRow == null) {
				logger.error("Uploaded sheet has no header row; nothing to match");
				return;
			}

			// ---- Pass 1: collect every name that needs parsing, then parse in bulk ----
			Set<String> namesToParse = new LinkedHashSet<>();
			for (Row row : sheet) {
				if (row.getRowNum() == 0) {
					continue;
				}
				Cell cell = row.getCell(index);
				if (cell == null) {
					continue;
				}
				namesToParse.add(cell.toString());

				if (synonym != -1) {
					Cell synonymcell = row.getCell(synonym);
					if (synonymcell != null) {
						for (String syn : synonymcell.toString().split("\\s*\\|\\s*")) {
							namesToParse.add(syn);
						}
					}
				}

				for (Integer hierIndex : rankMap.keySet()) {
					Cell hiercell = row.getCell(hierIndex);
					if (hiercell != null) {
						namesToParse.add(hiercell.toString());
					}
				}
			}
			namesToParse.removeIf(n -> n == null || n.isBlank());
			parsedNames = bulkParseNames(namesToParse);

			// ---- Pass 2: collect every (cell, canonical) pair that needs matching, then
			// match in bulk ----
			Map<String, TaxonMatchQuery> matchQueries = new LinkedHashMap<>();
			for (Row row : sheet) {
				if (row.getRowNum() == 0) {
					continue;
				}
				Cell cell = row.getCell(index);
				if (cell == null) {
					continue;
				}
				String cellText = cell.toString();

				// Synonyms
				if (synonym != -1) {
					Cell synonymcell = row.getCell(synonym);
					if (synonymcell != null) {
						for (String syn : synonymcell.toString().split("\\s*\\|\\s*")) {
							addMatchQuery(matchQueries, cellText, canonicalOf(parsedNames, syn));
						}
					}
				}

				// Main name
				addMatchQuery(matchQueries, cellText, canonicalOf(parsedNames, cellText));

				// Rank columns (first occurrence of each canonical only)
				for (Integer hierIndex : rankMap.keySet()) {
					Cell hiercell = row.getCell(hierIndex);
					if (hiercell != null) {
						String hierCanonical = canonicalOf(parsedNames, hiercell.toString());
						if (hierCanonical != null && !hierFirstCell.containsKey(hierCanonical)) {
							hierFirstCell.put(hierCanonical, cellText);
							addMatchQuery(matchQueries, cellText, hierCanonical);
						}
					}
				}
			}
			matchResults = bulkMatchNames(matchQueries);

			// ---- Species ids for every matched taxon (one bulk call) ----
			Set<Long> matchedTaxonIds = new LinkedHashSet<>();
			for (List<Object> matchList : matchResults.values()) {
				for (Object match : matchList) {
					Long id = taxonIdOf(match);
					if (id != null) {
						matchedTaxonIds.add(id);
					}
				}
			}
			speciesIds = fetchSpeciesIds(new ArrayList<>(matchedTaxonIds));

			// ---- Pass 3: move the original columns right and write the results at the
			// front of the copy ----
			List<Integer> rankCols = new ArrayList<>(new TreeSet<>(rankMap.keySet()));
			List<String> resultHeaders = buildResultHeaders(rankCols);
			shiftOriginalColumns(sheet, resultHeaders.size());
			writeResultHeaders(workbook, sheet.getRow(0), resultHeaders);
			startDetailsSheet(workbook);

			for (Row row : sheet) {
				if (row.getRowNum() == 0) {
					continue;
				}
				try {
					writeRowResults(row, 0, rankCols);
				} catch (Exception e) {
					logger.error("Failed to write match results for row {}: {}", row.getRowNum(), e.getMessage(), e);
				}
			}
			writeMetadata(workbook, resultHeaders.size());
			workbook.setSheetHidden(workbook.getSheetIndex(detailsSheet), true);

			// ---- Save the copy ----
			File out = resolveOutputFile();
			try (FileOutputStream fos = new FileOutputStream(out)) {
				workbook.write(fos);
			}
			outputFile = out;
			logger.info("Name matching results written to {}", out.getAbsolutePath());

		} catch (Exception e) {
			logger.error("Name matching failed: {}", e.getMessage(), e);
		} finally {
			fileBytes = null; // release the in-memory copy of the upload
		}
	}

	// ------------------------------------------------------------------------
	// Writing result columns
	// ------------------------------------------------------------------------

	/**
	 * Moves every original column right by {@code count}, keeping column widths,
	 * so the result columns can go in columns 0..count-1.
	 */
	private void shiftOriginalColumns(Sheet sheet, int count) {
		int lastCol = -1;
		for (Row row : sheet) {
			lastCol = Math.max(lastCol, row.getLastCellNum() - 1);
		}
		if (lastCol >= 0) {
			int[] widths = new int[lastCol + 1];
			for (int c = 0; c <= lastCol; c++) {
				widths[c] = sheet.getColumnWidth(c);
			}
			sheet.shiftColumns(0, lastCol, count);
			for (int c = 0; c <= lastCol; c++) {
				sheet.setColumnWidth(c + count, widths[c]);
			}
		}
		for (int c = 0; c < count; c++) {
			sheet.setColumnWidth(c, 20 * 256);
		}
		colOffset = count;
	}

	/** Cell of an ORIGINAL column (by its index in the uploaded file). */
	private Cell origCell(Row row, int originalCol) {
		return row.getCell(originalCol + colOffset);
	}

	/** Creates the (later hidden) sheet holding full synonym / rank match details. */
	private void startDetailsSheet(Workbook workbook) {
		int existing = workbook.getSheetIndex(DETAILS_SHEET);
		if (existing >= 0) {
			workbook.removeSheetAt(existing);
		}
		detailsSheet = workbook.createSheet(DETAILS_SHEET);
		Row header = detailsSheet.createRow(0);
		header.createCell(0).setCellValue("row");
		header.createCell(1).setCellValue("kind");
		header.createCell(2).setCellValue("name");
		header.createCell(3).setCellValue("matches (JSON, may continue in next columns)");
		detailsRowNum = 1;
	}

	/**
	 * Saves every field the old response had for these matches (position, group,
	 * hierarchy, accepted ids, ...) as JSON, so the reader can restore them
	 * exactly. Long JSON is split across columns to stay under the cell limit.
	 */
	private void addDetail(int dataRowNum, String kind, String name, List<Object> matches) {
		try {
			List<Map<String, Object>> slim = new ArrayList<>();
			for (Object match : matches) {
				Map<?, ?> m = (Map<?, ?>) match;
				Long id = taxonIdOf(match);
				Map<String, Object> s = new LinkedHashMap<>();
				s.put("name", m.get("name"));
				s.put("rank", m.get("rank"));
				s.put("status", m.get("status"));
				s.put("position", m.get("position"));
				s.put("group_name", m.get("group_name"));
				s.put("hierarchy", m.get("hierarchy"));
				s.put("id", id);
				s.put("acceptedId", m.get("accepted_ids"));
				s.put("speciesId", id == null ? null : speciesIds.get(id));
				slim.add(s);
			}
			String json = JSON.writeValueAsString(slim);

			Row r = detailsSheet.createRow(detailsRowNum++);
			r.createCell(0).setCellValue(String.valueOf(dataRowNum));
			r.createCell(1).setCellValue(kind);
			r.createCell(2).setCellValue(name);
			int col = 3;
			for (int start = 0; start < json.length(); start += DETAIL_CHUNK) {
				r.createCell(col++).setCellValue(json.substring(start, Math.min(start + DETAIL_CHUNK, json.length())));
			}
		} catch (Exception e) {
			logger.warn("Could not save {} details for '{}' in row {}: {}", kind, name, dataRowNum, e.getMessage());
		}
	}

	/** Saves the processing settings in a hidden sheet of the copy. */
	private void writeMetadata(Workbook workbook, int resultColumnCount) {
		int existing = workbook.getSheetIndex(META_SHEET);
		if (existing >= 0) {
			workbook.removeSheetAt(existing);
		}
		Sheet meta = workbook.createSheet(META_SHEET);
		String[][] entries = { { META_VERSION, META_VERSION_VALUE }, { META_INDEX, String.valueOf(index) },
				{ META_SYNONYM, String.valueOf(synonym) }, { META_CNAME, String.valueOf(cname) },
				{ META_RANK, rank == null ? "" : rank }, { META_RESULT_COLUMNS, String.valueOf(resultColumnCount) } };
		for (int i = 0; i < entries.length; i++) {
			Row r = meta.createRow(i);
			r.createCell(0).setCellValue(entries[i][0]);
			r.createCell(1).setCellValue(entries[i][1]);
		}
		workbook.setSheetHidden(workbook.getSheetIndex(meta), true);
	}

	private List<String> buildResultHeaders(List<Integer> rankCols) {
		List<String> resultHeaders = new ArrayList<>(List.of(MAIN_RESULT_COLUMNS));
		if (synonym != -1) {
			resultHeaders.add("Synonym Matches");
		}
		if (cname != -1) {
			resultHeaders.add("Common Name Matches");
			resultHeaders.add("Unmatched Common Names");
		}
		for (Integer hierIndex : rankCols) {
			resultHeaders.add(rankMap.get(hierIndex) + " Match");
		}
		return resultHeaders;
	}

	private void writeResultHeaders(Workbook workbook, Row headersRow, List<String> resultHeaders) {
		CellStyle headerStyle = workbook.createCellStyle();
		Font bold = workbook.createFont();
		bold.setBold(true);
		headerStyle.setFont(bold);

		int col = 0;
		for (String header : resultHeaders) {
			Cell c = headersRow.createCell(col++);
			c.setCellValue(header);
			c.setCellStyle(headerStyle);
		}
	}

	private void writeRowResults(Row row, int startCol, List<Integer> rankCols) {
		int col = startCol;
		Cell cell = origCell(row, index);
		String cellText = cell == null ? "" : cell.toString();

		// ---- Main name ----
		List<Object> matches = new ArrayList<>();
		String status;
		if (cellText.isBlank()) {
			status = "No name";
		} else {
			String canonical = canonicalOf(parsedNames, cellText);
			if (canonical == null) {
				status = "Could not parse";
			} else {
				matches = lookupMatches(matchResults, cellText, canonical);
				if (matches.isEmpty()) {
					status = "No match";
				} else if (matches.size() == 1) {
					status = "Single match";
				} else {
					status = "Multiple matches";
				}
			}
		}

		setText(row, col++, status);
		row.createCell(col++).setCellValue(matches.size());
		setText(row, col++, joinField(matches, "name"));
		setText(row, col++, joinField(matches, "rank"));
		setText(row, col++, joinField(matches, "status"));
		setText(row, col++, joinField(matches, "position"));
		setText(row, col++, joinField(matches, "group_name"));

		List<String> hierarchies = new ArrayList<>();
		List<String> ids = new ArrayList<>();
		List<String> sids = new ArrayList<>();
		for (Object match : matches) {
			String h = formatHierarchy(((Map<?, ?>) match).get("hierarchy"));
			hierarchies.add(h.isEmpty() ? MISSING : h);
			Long id = taxonIdOf(match);
			ids.add(id == null ? MISSING : id.toString());
			Long sid = id == null ? null : speciesIds.get(id);
			sids.add(sid == null ? MISSING : sid.toString());
		}
		setText(row, col++, String.join(MATCH_SEPARATOR, hierarchies));
		setText(row, col++, String.join(MATCH_SEPARATOR, ids));
		setText(row, col++, joinField(matches, "accepted_ids"));
		setText(row, col++, String.join(MATCH_SEPARATOR, sids));

		// ---- Synonyms ----
		if (synonym != -1) {
			List<String> parts = new ArrayList<>();
			Cell synCell = origCell(row, synonym);
			if (synCell != null && !cellText.isBlank()) {
				for (String syn : synCell.toString().split("\\s*\\|\\s*")) {
					if (syn.isBlank()) {
						continue;
					}
					String canonical = canonicalOf(parsedNames, syn);
					if (canonical == null) {
						parts.add(syn + " => could not parse");
						continue;
					}
					List<Object> synMatches = lookupMatches(matchResults, cellText, canonical);
					parts.add(syn + " => " + (synMatches.isEmpty() ? "no match" : describeMatches(synMatches)));
					if (!synMatches.isEmpty()) {
						addDetail(row.getRowNum(), DETAIL_SYNONYM, syn, synMatches);
					}
				}
			}
			setText(row, col++, String.join(MATCH_SEPARATOR, parts));
		}

		// ---- Common names (checked against the main name's matches) ----
		if (cname != -1) {
			List<String> matched = new ArrayList<>();
			List<String> unmatched = new ArrayList<>();
			Cell cnCell = origCell(row, cname);
			if (cnCell != null) {
				Map<String, Set<Long>> cnameIndex = buildCommonNameIndex(matches);
				for (String[] langAndName : parseCommonNames(cnCell.toString(), row.getRowNum())) {
					String lang = langAndName[0];
					String name = langAndName[1];
					Set<Long> cnIds = cnameIndex.get(name + "#" + lang);
					if (cnIds != null) {
						matched.add(lang + ": " + name + " (id=" + joinLongs(cnIds) + ")");
					} else {
						unmatched.add(lang + ": " + name);
					}
				}
			}
			setText(row, col++, String.join(MATCH_SEPARATOR, matched));
			setText(row, col++, String.join(MATCH_SEPARATOR, unmatched));
		}

		// ---- Rank / hierarchy columns ----
		for (Integer hierIndex : rankCols) {
			Cell hierCell = origCell(row, hierIndex);
			String value = "";
			if (hierCell != null && !hierCell.toString().isBlank()) {
				String canonical = canonicalOf(parsedNames, hierCell.toString());
				if (canonical == null) {
					value = "Could not parse";
				} else {
					List<Object> hierMatches = lookupMatches(matchResults,
							hierFirstCell.getOrDefault(canonical, cellText), canonical);
					value = hierMatches.isEmpty() ? "No match" : describeMatches(hierMatches);
					if (!hierMatches.isEmpty()) {
						addDetail(row.getRowNum(), DETAIL_RANK, rankMap.get(hierIndex), hierMatches);
					}
				}
			}
			setText(row, col++, value);
		}
	}

	/** common name + "#" + language -> ids, across all given matches. */
	private Map<String, Set<Long>> buildCommonNameIndex(List<Object> matches) {
		Map<String, Set<Long>> index = new HashMap<>();
		for (Object match : matches) {
			Object cnames = ((Map<?, ?>) match).get("common_names");
			if (!(cnames instanceof Collection)) {
				continue;
			}
			for (Object nameEntry : (Collection<?>) cnames) {
				if (!(nameEntry instanceof Map)) {
					continue;
				}
				Map<?, ?> nameMap = (Map<?, ?>) nameEntry;
				Object idObj = nameMap.get("id");
				Object nameObj = nameMap.get("name");
				Object langObj = nameMap.get("language_name");
				if (!(idObj instanceof Number) || nameObj == null || langObj == null) {
					logger.warn("Skipping common_names entry with missing field(s): {}", nameMap);
					continue;
				}
				String key = nameObj.toString().trim() + "#" + langObj.toString().trim();
				index.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(((Number) idObj).longValue());
			}
		}
		return index;
	}

	/**
	 * Parses "Lang1: cn1, cn2, cn3 | Lang2: cn1" into [lang, name] pairs, skipping
	 * empty or malformed segments.
	 */
	private List<String[]> parseCommonNames(String cellText, int rowNum) {
		List<String[]> result = new ArrayList<>();
		for (String langGroup : cellText.split("\\s*\\|\\s*")) {
			if (langGroup.trim().isEmpty()) {
				continue;
			}
			String[] cnamedet = langGroup.split("\\s*:\\s*", 2);
			if (cnamedet.length < 2) {
				logger.warn("Skipping malformed common name entry '{}' in cell for row {}", langGroup, rowNum);
				continue;
			}
			String lang = cnamedet[0].trim();
			for (String rawName : cnamedet[1].split("\\s*,\\s*")) {
				String name = rawName.trim();
				if (!name.isEmpty()) {
					result.add(new String[] { lang, name });
				}
			}
		}
		return result;
	}

	/** "Name (rank, status, id=1, speciesId=2); Name2 (...)" */
	private String describeMatches(List<Object> matches) {
		List<String> parts = new ArrayList<>();
		for (Object match : matches) {
			Map<?, ?> m = (Map<?, ?>) match;
			Long id = taxonIdOf(match);
			Long sid = id == null ? null : speciesIds.get(id);
			StringBuilder sb = new StringBuilder();
			sb.append(fmt(m.get("name"))).append(" (").append(fmt(m.get("rank"))).append(", ")
					.append(fmt(m.get("status"))).append(", id=").append(id == null ? MISSING : id);
			if (sid != null) {
				sb.append(", speciesId=").append(sid);
			}
			sb.append(")");
			parts.add(sb.toString());
		}
		return String.join("; ", parts);
	}

	/** One value per match for the given key, aligned by position. */
	private static String joinField(List<Object> matches, String key) {
		List<String> parts = new ArrayList<>();
		for (Object match : matches) {
			String v = fmt(((Map<?, ?>) match).get(key));
			parts.add(v.isEmpty() ? MISSING : v);
		}
		return String.join(MATCH_SEPARATOR, parts);
	}

	private static String fmt(Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof Collection) {
			List<String> parts = new ArrayList<>();
			for (Object o : (Collection<?>) value) {
				parts.add(fmt(o));
			}
			return String.join(",", parts);
		}
		return value.toString();
	}

	/**
	 * Hierarchy as "Animalia [kingdom, id=872] > Chordata [phylum, id=20218] > ...".
	 * Keeps name, rank and id of every node so the reader can restore it exactly.
	 */
	private static String formatHierarchy(Object hierarchy) {
		if (!(hierarchy instanceof Collection)) {
			return fmt(hierarchy);
		}
		List<String> parts = new ArrayList<>();
		for (Object node : (Collection<?>) hierarchy) {
			if (node instanceof Map) {
				Map<?, ?> m = (Map<?, ?>) node;
				Object name = m.get("taxon_name");
				if (name == null) {
					name = m.get("normalized_name");
				}
				if (name == null) {
					name = m.get("name");
				}
				if (name == null) {
					parts.add(m.toString());
					continue;
				}
				Object nodeRank = m.get("taxon_rank");
				Object nodeId = m.get("taxon_id");
				parts.add(name + " [" + fmt(nodeRank) + ", id=" + (nodeId == null ? MISSING : nodeId) + "]");
			} else if (node != null) {
				parts.add(node.toString());
			}
		}
		return String.join(" > ", parts);
	}

	private static String joinLongs(Collection<Long> values) {
		List<String> parts = new ArrayList<>();
		for (Long v : values) {
			parts.add(v.toString());
		}
		return String.join(",", parts);
	}

	private static Long taxonIdOf(Object match) {
		Object idObj = ((Map<?, ?>) match).get("id");
		return idObj instanceof Number ? ((Number) idObj).longValue() : null;
	}

	private static void setText(Row row, int col, String value) {
		String v = value == null ? "" : value;
		if (v.length() > MAX_CELL_LENGTH) {
			v = v.substring(0, MAX_CELL_LENGTH - 3) + "...";
		}
		row.createCell(col).setCellValue(v);
	}

	/**
	 * basePath + modulePath + File.separator + fileName (falls back to the system
	 * temp directory if no basePath was given). Creates missing folders.
	 */
	private File resolveOutputFile() throws IOException {
		String fileName = buildFileName();
		if (basePath == null || basePath.isBlank()) {
			return new File(System.getProperty("java.io.tmpdir"), fileName);
		}
		String modulePathForDownloads = modulePath == null ? "" : modulePath;
		String filePath = basePath + modulePathForDownloads + File.separator + fileName;
		File out = new File(filePath);
		File dir = out.getParentFile();
		if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
			throw new IOException("Could not create folder " + dir.getAbsolutePath());
		}
		return out;
	}

	/** "<uploaded file name>_matched_<yyyyMMdd_HHmmss>.xlsx" */
	private String buildFileName() {
		String base = "name_matching";
		if (uploadedFileName != null && !uploadedFileName.isBlank()) {
			base = uploadedFileName.replaceFirst("\\.[^.]+$", "");
		}
		base = base.replaceAll("[^A-Za-z0-9._-]", "_");
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
		return base + "_matched_" + timestamp + ".xlsx";
	}

	// ------------------------------------------------------------------------
	// Bulk lookups (unchanged)
	// ------------------------------------------------------------------------

	/** Key for a (scientificText, canonicalText) pair in the match-result map. */
	private static String matchKey(String scientificText, String canonicalText) {
		return scientificText + "\u0000" + canonicalText;
	}

	/** Simple canonical form from the bulk-parse map, or null if unparsed. */
	private static String canonicalOf(Map<String, ParsedName> parsedNames, String name) {
		ParsedName p = parsedNames.get(name);
		if (p == null || p.getCanonical() == null) {
			return null;
		}
		String simple = p.getCanonical().getSimple();
		return (simple == null || simple.isBlank()) ? null : simple;
	}

	/**
	 * Adds a (scientificText, canonicalText) pair to the bulk match request, once.
	 */
	private static void addMatchQuery(Map<String, TaxonMatchQuery> queries, String scientificText,
			String canonicalText) {
		if (canonicalText == null) {
			return;
		}
		queries.computeIfAbsent(matchKey(scientificText, canonicalText), k -> {
			TaxonMatchQuery q = new TaxonMatchQuery();
			q.setScientificText(scientificText);
			q.setCanonicalText(canonicalText);
			return q;
		});
	}

	/**
	 * Runs all match queries via the bulk match endpoint, in batches. Returns
	 * matchKey -> list of matches (same shape as esServicesApi.match). Queries with
	 * no matches, or whose batch failed, are absent from the map.
	 */
	@SuppressWarnings("unchecked")
	private Map<String, List<Object>> bulkMatchNames(Map<String, TaxonMatchQuery> queries) {
		Map<String, List<Object>> results = new HashMap<>();
		List<TaxonMatchQuery> queryList = new ArrayList<>(queries.values());

		for (int start = 0; start < queryList.size(); start += NAME_MATCH_BATCH_SIZE) {
			List<TaxonMatchQuery> chunk = new ArrayList<>(
					queryList.subList(start, Math.min(start + NAME_MATCH_BATCH_SIZE, queryList.size())));
			try {
				// Endpoint is declared as Object in Swagger; actual body is a list of lists
				Object raw = esServicesApi.bulkMatch(chunk);
				if (!(raw instanceof List)) {
					logger.warn("Bulk match returned unexpected type for batch starting at {}", start);
					continue;
				}
				List<List<Object>> matched = (List<List<Object>>) raw;
				if (matched.size() != chunk.size()) {
					logger.warn("Bulk match returned {} results for {} queries", matched.size(), chunk.size());
				}
				for (int i = 0; i < Math.min(matched.size(), chunk.size()); i++) {
					List<Object> m = matched.get(i);
					if (m != null && !m.isEmpty()) {
						TaxonMatchQuery q = chunk.get(i);
						results.put(matchKey(q.getScientificText(), q.getCanonicalText()), m);
					}
				}
			} catch (Exception e) {
				logger.error("Bulk match failed for batch starting at {}: {}", start, e.getMessage());
			}
		}
		return results;
	}

	/** Matches for a pair from the bulk results; empty list if none. */
	private static List<Object> lookupMatches(Map<String, List<Object>> matchResults, String scientificText,
			String canonicalText) {
		List<Object> m = matchResults.get(matchKey(scientificText, canonicalText));
		return m != null ? m : new ArrayList<>();
	}

	/**
	 * taxonConceptId -> speciesId for the given taxon ids, via the bulk species-id
	 * endpoint. Taxa with no species (or a failed call) are absent from the map.
	 */
	private Map<Long, Long> fetchSpeciesIds(List<Long> taxonConceptIds) {
		Map<Long, Long> result = new HashMap<>();
		if (taxonConceptIds.isEmpty()) {
			return result;
		}
		try {
			// Endpoint is declared as Object in Swagger; JSON keys arrive as strings
			Object raw = esServicesApi.getBulkSpeciesIds(taxonConceptIds);
			if (raw instanceof Map) {
				for (Map.Entry<?, ?> e : ((Map<?, ?>) raw).entrySet()) {
					if (e.getKey() != null && e.getValue() instanceof Number) {
						result.put(Long.valueOf(e.getKey().toString()), ((Number) e.getValue()).longValue());
					}
				}
			} else {
				logger.warn("Bulk species id lookup returned unexpected type");
			}
		} catch (Exception e) {
			logger.error("Bulk species id lookup failed for {} taxa: {}", taxonConceptIds.size(), e.getMessage());
		}
		return result;
	}

	private Map<String, ParsedName> bulkParseNames(Collection<String> names) {
		Map<String, ParsedName> result = new HashMap<>();
		List<String> nameList = new ArrayList<>(names);

		for (int start = 0; start < nameList.size(); start += NAME_PARSE_BATCH_SIZE) {
			List<String> chunk = new ArrayList<>(
					nameList.subList(start, Math.min(start + NAME_PARSE_BATCH_SIZE, nameList.size())));
			try {
				List<ParsedName> parsed = utilityServiceApi.getBulkNameParsed(chunk);
				if (parsed == null) {
					logger.warn("Bulk name parser returned null for batch starting at {}", start);
					continue;
				}
				if (parsed.size() != chunk.size()) {
					logger.warn("Bulk name parser returned {} results for {} names", parsed.size(), chunk.size());
				}
				for (int i = 0; i < Math.min(parsed.size(), chunk.size()); i++) {
					if (parsed.get(i) != null) {
						result.put(chunk.get(i), parsed.get(i));
					}
				}
			} catch (Exception e) {
				logger.error("Bulk name parsing failed for batch starting at {}: {}", start, e.getMessage());
			}
		}
		return result;
	}

}