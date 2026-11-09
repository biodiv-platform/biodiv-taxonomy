package com.strandls.taxonomy.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strandls.taxonomy.pojo.response.NameMatching;

/**
 * Turns a workbook written by {@link TaxonomyNameMatchingThread} (original
 * columns + appended result columns) back into the {@link NameMatching}
 * response the thread used to build: headers, data, synonym, cname and
 * hierarchy, with the same keys as before.
 *
 * Only the file is needed: the result columns are at the front of the first
 * sheet, and the settings used to process it (index, synonym, cname, rank) are
 * read from the hidden {@value TaxonomyNameMatchingThread#META_SHEET} sheet.
 *
 * Synonym and rank-column matches are read with all their fields (position,
 * group_name, hierarchy, acceptedId, ...) from the hidden
 * {@value TaxonomyNameMatchingThread#DETAILS_SHEET} sheet. Files processed
 * before that sheet existed fall back to the summary text, where only name,
 * rank, status, id and speciesId are available.
 */
public final class NameMatchingResultReader {

	private static final Logger logger = LoggerFactory.getLogger(NameMatchingResultReader.class);

	// Must match the headers and formats written by TaxonomyNameMatchingThread
	static final String COL_MATCH_STATUS = "Match Status";
	static final String COL_MATCH_COUNT = "Match Count";
	static final String COL_NAME = "Matched Name";
	static final String COL_RANK = "Matched Rank";
	static final String COL_STATUS = "Matched Status";
	static final String COL_POSITION = "Position";
	static final String COL_GROUP = "Group";
	static final String COL_HIERARCHY = "Hierarchy";
	static final String COL_TAXON_ID = "Taxon Id";
	static final String COL_ACCEPTED_ID = "Accepted Id";
	static final String COL_SPECIES_ID = "Species Id";
	static final String COL_SYNONYMS = "Synonym Matches";
	static final String COL_CNAME_MATCHES = "Common Name Matches";
	static final String RANK_COL_SUFFIX = " Match";

	private static final String[] REQUIRED_COLUMNS = { COL_MATCH_STATUS, COL_MATCH_COUNT, COL_NAME, COL_RANK,
			COL_STATUS, COL_POSITION, COL_GROUP, COL_HIERARCHY, COL_TAXON_ID, COL_ACCEPTED_ID, COL_SPECIES_ID };

	private static final String MATCH_SEPARATOR = " | ";
	private static final String MISSING = "-";

	/** "Name (rank, status, id=1, speciesId=2)" followed by "; " or end of text. */
	private static final Pattern DESCRIBED_MATCH = Pattern
			.compile("(.+?) \\(([^,()]*), ([^,()]*), id=(\\d+|-)(?:, speciesId=(\\d+))?\\)(?:; |$)");

	/** "Lang: name (id=1,2)" */
	private static final Pattern CNAME_MATCH = Pattern.compile("^(.*?): (.*) \\(id=([\\d,]+)\\)$");

	private NameMatchingResultReader() {
	}

	/**
	 * @throws IllegalArgumentException if the file is not a processed
	 *                                  name-matching file
	 */
	public static NameMatching read(InputStream inputStream) throws IOException {

		try (Workbook workbook = new XSSFWorkbook(inputStream)) {

			// ---- Settings saved by the thread in the hidden sheet ----
			Sheet metaSheet = workbook.getSheet(TaxonomyNameMatchingThread.META_SHEET);
			if (metaSheet == null) {
				throw new IllegalArgumentException("This is not a processed name-matching file (no '"
						+ TaxonomyNameMatchingThread.META_SHEET + "' sheet). Files processed before the"
						+ " result columns moved to the front have to be processed again.");
			}
			Map<String, String> meta = new HashMap<>();
			for (Row r : metaSheet) {
				String key = text(r.getCell(0)).trim();
				if (!key.isEmpty()) {
					meta.put(key, text(r.getCell(1)).trim());
				}
			}
			int index = metaInt(meta, TaxonomyNameMatchingThread.META_INDEX, null);
			int synonym = metaInt(meta, TaxonomyNameMatchingThread.META_SYNONYM, -1);
			int cname = metaInt(meta, TaxonomyNameMatchingThread.META_CNAME, -1);
			int resultColumnCount = metaInt(meta, TaxonomyNameMatchingThread.META_RESULT_COLUMNS, null);
			String rank = meta.get(TaxonomyNameMatchingThread.META_RANK);

			// The data is always on the first sheet (the meta sheet is added after it)
			Sheet sheet = workbook.getSheetAt(0);
			Row headersRow = sheet.getRow(0);
			if (headersRow == null) {
				throw new IllegalArgumentException("The file has no header row");
			}

			// Result columns are 0..resultColumnCount-1; original columns follow
			List<String> headers = new ArrayList<>();
			Map<String, Integer> resultCols = new HashMap<>();
			for (Cell header : headersRow) {
				if (header.getColumnIndex() < resultColumnCount) {
					resultCols.putIfAbsent(text(header), header.getColumnIndex());
				} else {
					headers.add(text(header));
				}
			}
			for (String required : REQUIRED_COLUMNS) {
				if (!resultCols.containsKey(required)) {
					throw new IllegalArgumentException("Missing result column '" + required + "'");
				}
			}

			Map<Integer, String> rankMap = new HashMap<>();
			if (rank != null && !rank.isBlank()) {
				for (String ranki : rank.split("\\|")) {
					String[] rankDet = ranki.split("\\s*:\\s*");
					if (rankDet.length < 2) {
						throw new IllegalArgumentException("Malformed rank parameter: '" + ranki + "'");
					}
					rankMap.put(Integer.valueOf(rankDet[1].trim()), rankDet[0].trim());
				}
			}

			Builder builder = new Builder(headers, resultCols, rankMap, index, synonym, cname, resultColumnCount,
					readDetails(workbook));
			for (Row row : sheet) {
				if (row.getRowNum() == 0) {
					continue;
				}
				try {
					builder.addRow(row);
				} catch (RuntimeException e) {
					logger.error("Could not rebuild results for row {}: {}", row.getRowNum(), e.getMessage(), e);
				}
			}

			NameMatching result = new NameMatching();
			result.setHeaders(headers);
			result.setData(builder.values);
			result.setSynonym(builder.synonymMap);
			result.setCname(builder.commonNameMap);
			result.setHierarchy(builder.hierMap);
			// Scientific name column, as an index into headers (the original columns)
			result.setColumn(index);
			return result;
		}
	}

	/** Holds the maps being filled while walking the rows. */
	private static final class Builder {
		private final List<String> headers;
		private final Map<String, Integer> resultCols;
		private final Map<Integer, String> rankMap;
		private final int index;
		private final int synonym;
		private final int cname;
		/** Original column N is at N + offset in the processed file. */
		private final int offset;
		/** Full synonym / rank match details from the hidden sheet: row#kind#name -> matches. */
		private final Map<String, List<Map<String, Object>>> details;

		final List<Map<String, Object>> values = new ArrayList<>();
		final Map<String, Object> synonymMap = new LinkedHashMap<>();
		final Map<String, Object> commonNameMap = new LinkedHashMap<>();
		final Map<String, Object> hierMap = new LinkedHashMap<>();
		/** Same role as matchMap in the old code: first result per rank-column value is reused. */
		private final Map<String, List<Object>> hierCache = new HashMap<>();

		Builder(List<String> headers, Map<String, Integer> resultCols, Map<Integer, String> rankMap, int index,
				int synonym, int cname, int offset, Map<String, List<Map<String, Object>>> details) {
			this.headers = headers;
			this.resultCols = resultCols;
			this.rankMap = rankMap;
			this.index = index;
			this.synonym = synonym;
			this.cname = cname;
			this.offset = offset;
			this.details = details;
		}

		/** Cell of an ORIGINAL column (by its index in the uploaded file). */
		private Cell orig(Row row, int originalCol) {
			return row.getCell(originalCol + offset);
		}

		void addRow(Row row) {
			Cell cell = orig(row, index);
			if (cell == null) {
				return; // the old code skipped these rows too
			}
			String cellText = cell.toString();
			int rowNum = row.getRowNum();

			StringBuilder rowValues = new StringBuilder();
			for (int i = 0; i < headers.size(); i++) {
				Cell c = orig(row, i);
				rowValues.append(c != null ? c.toString() : "").append("#");
			}

			String status = resultText(row, COL_MATCH_STATUS);
			boolean parsed = !("Could not parse".equals(status) || "No name".equals(status));

			// ---- Synonyms (old code handled these whether or not the main name parsed) ----
			if (synonym != -1 && resultCols.containsKey(COL_SYNONYMS)) {
				Cell synCell = orig(row, synonym);
				if (synCell != null) {
					Map<String, String> synResults = parseSynonymColumn(resultText(row, COL_SYNONYMS));
					for (String syn : synCell.toString().split("\\s*\\|\\s*")) {
						String r = synResults.get(syn);
						if (r == null || "could not parse".equals(r)) {
							continue; // old code: unparsed synonyms got no entry
						}
						List<Object> matches = "no match".equals(r) ? new ArrayList<>()
								: detailedOr(rowNum, TaxonomyNameMatchingThread.DETAIL_SYNONYM, syn, cellText,
										r);
						synonymMap.put(syn + "#" + cellText + "#" + rowNum, matches);
					}
				}
			}

			// ---- Main name ----
			List<Object> optMatches = parsed ? readMainMatches(row) : new ArrayList<>();
			Map<String, Object> rowMap = new HashMap<>();
			rowMap.put(rowValues.toString(), optMatches);
			values.add(rowMap);

			// ---- Common names ----
			if (cname != -1) {
				Cell cnCell = orig(row, cname);
				if (cnCell != null) {
					if (parsed) {
						Map<String, Long> cnameIds = resultCols.containsKey(COL_CNAME_MATCHES)
								? parseCnameMatches(resultText(row, COL_CNAME_MATCHES))
								: new HashMap<>();
						for (String[] langAndName : parseCommonNames(cnCell.toString())) {
							String lang = langAndName[0];
							String name = langAndName[1];
							Map<String, Object> cnameMatch = new HashMap<>();
							Long id = cnameIds.get(name + "#" + lang);
							if (id != null) {
								cnameMatch.put("name", name);
								cnameMatch.put("lang", lang);
								cnameMatch.put("source", cellText);
								cnameMatch.put("id", id);
							}
							commonNameMap.put(name + "#" + cellText + "#" + lang + "#" + rowNum, cnameMatch);
						}
					} else {
						// Old "could not parse" branch: everything after "Lang:" was one name
						for (String commonName : cnCell.toString().split("\\s*\\|\\s*")) {
							if (commonName.trim().isEmpty()) {
								continue;
							}
							String[] cnamedet = commonName.split("\\s*:\\s*", 2);
							if (cnamedet.length < 2) {
								continue;
							}
							commonNameMap.put(cnamedet[1].trim() + "#" + cellText + "#" + cnamedet[0].trim() + "#"
									+ rowNum, new HashMap<String, Object>());
						}
					}
				}
			}

			// ---- Rank columns ----
			for (Integer hierIndex : rankMap.keySet()) {
				Cell hierCell = orig(row, hierIndex);
				if (hierCell == null) {
					continue;
				}
				String rankName = rankMap.get(hierIndex);
				Integer col = resultCols.get(rankName + RANK_COL_SUFFIX);
				if (col == null) {
					continue;
				}
				String v = text(row.getCell(col));
				if (v.isEmpty() || "Could not parse".equals(v)) {
					continue;
				}
				String hierText = hierCell.toString();
				List<Object> matches = hierCache.get(hierText);
				if (matches == null) {
					matches = "No match".equals(v) ? new ArrayList<>()
							: detailedOr(rowNum, TaxonomyNameMatchingThread.DETAIL_RANK, rankName, cellText, v);
					hierCache.put(hierText, matches);
				}
				hierMap.put(hierText + "#" + cellText + "#" + rankName + "#" + rowNum, matches);
			}
		}

		/**
		 * Matches from the hidden details sheet (all fields) if present, otherwise
		 * parsed from the summary text (name, rank, status, id, speciesId only).
		 */
		private List<Object> detailedOr(int rowNum, String kind, String name, String source, String summary) {
			List<Map<String, Object>> full = details.get(rowNum + "#" + kind + "#" + name);
			if (full == null) {
				return parseDescribedMatches(summary, source);
			}
			List<Object> matches = new ArrayList<>();
			for (Map<String, Object> d : full) {
				// Same fields and order as the old response for synonym / hierarchy matches
				Map<String, Object> optMatch = new LinkedHashMap<>();
				optMatch.put("name", d.get("name"));
				optMatch.put("rank", d.get("rank"));
				optMatch.put("status", d.get("status"));
				optMatch.put("position", d.get("position"));
				optMatch.put("group_name", d.get("group_name"));
				optMatch.put("hierarchy", d.get("hierarchy"));
				optMatch.put("id", d.get("id") instanceof Number ? ((Number) d.get("id")).longValue() : null);
				optMatch.put("source", source);
				optMatch.put("acceptedId", d.get("acceptedId"));
				optMatch.put("speciesId",
						d.get("speciesId") instanceof Number ? ((Number) d.get("speciesId")).longValue() : null);
				matches.add(optMatch);
			}
			return matches;
		}

		private List<Object> readMainMatches(Row row) {
			int n = matchCount(row);
			List<String> names = perMatch(row, COL_NAME, n);
			List<String> ranks = perMatch(row, COL_RANK, n);
			List<String> statuses = perMatch(row, COL_STATUS, n);
			List<String> positions = perMatch(row, COL_POSITION, n);
			List<String> groups = perMatch(row, COL_GROUP, n);
			List<String> hierarchies = perMatch(row, COL_HIERARCHY, n);
			List<String> acceptedIds = perMatch(row, COL_ACCEPTED_ID, n);
			List<String> ids = perMatch(row, COL_TAXON_ID, n);
			List<String> speciesIds = perMatch(row, COL_SPECIES_ID, n);

			List<Object> optMatches = new ArrayList<>();
			for (int i = 0; i < n; i++) {
				Map<String, Object> optMatch = new LinkedHashMap<>();
				optMatch.put("name", names.get(i));
				optMatch.put("rank", ranks.get(i));
				optMatch.put("status", statuses.get(i));
				optMatch.put("position", positions.get(i));
				optMatch.put("group_name", groups.get(i));
				optMatch.put("hierarchy", toHierarchy(hierarchies.get(i)));
				optMatch.put("acceptedId", toIdList(acceptedIds.get(i)));
				optMatch.put("id", toLong(ids.get(i)));
				optMatch.put("speciesId", toLong(speciesIds.get(i)));
				optMatches.add(optMatch);
			}
			return optMatches;
		}

		private int matchCount(Row row) {
			Cell c = row.getCell(resultCols.get(COL_MATCH_COUNT));
			if (c != null && c.getCellType() == CellType.NUMERIC) {
				return (int) c.getNumericCellValue();
			}
			String t = text(c).trim();
			try {
				return t.isEmpty() ? 0 : (int) Double.parseDouble(t);
			} catch (NumberFormatException e) {
				return 0;
			}
		}

		/** n values from a per-match column; missing values become null. */
		private List<String> perMatch(Row row, String column, int n) {
			String s = resultText(row, column);
			String[] parts = n == 1 ? new String[] { s } : s.split(Pattern.quote(MATCH_SEPARATOR), -1);
			List<String> result = new ArrayList<>();
			for (int i = 0; i < n; i++) {
				String v = i < parts.length ? parts[i] : null;
				result.add(v == null || v.isEmpty() || MISSING.equals(v) ? null : v);
			}
			return result;
		}

		private String resultText(Row row, String column) {
			return text(row.getCell(resultCols.get(column)));
		}
	}

	private static final ObjectMapper JSON = new ObjectMapper();

	/** Reads the hidden details sheet, if the file has one: row#kind#name -> matches. */
	@SuppressWarnings("unchecked")
	private static Map<String, List<Map<String, Object>>> readDetails(Workbook workbook) {
		Map<String, List<Map<String, Object>>> details = new HashMap<>();
		Sheet sheet = workbook.getSheet(TaxonomyNameMatchingThread.DETAILS_SHEET);
		if (sheet == null) {
			return details; // older file: fall back to the summary text
		}
		for (Row r : sheet) {
			if (r.getRowNum() == 0) {
				continue;
			}
			String rowNum = text(r.getCell(0)).trim().replaceFirst("\\.0$", "");
			String kind = text(r.getCell(1));
			String name = text(r.getCell(2));
			StringBuilder json = new StringBuilder();
			for (int c = 3; c < r.getLastCellNum(); c++) {
				json.append(text(r.getCell(c)));
			}
			try {
				details.put(rowNum + "#" + kind + "#" + name, JSON.readValue(json.toString(), List.class));
			} catch (Exception e) {
				logger.warn("Could not read {} details for '{}' in row {}: {}", kind, name, rowNum, e.getMessage());
			}
		}
		return details;
	}

	// ------------------------------------------------------------------------
	// Parsing helpers (package-private for tests)
	// ------------------------------------------------------------------------

	/** "syn1 => ... | syn2 => no match" -> {syn1: "...", syn2: "no match"} */
	static Map<String, String> parseSynonymColumn(String text) {
		Map<String, String> result = new HashMap<>();
		if (text == null || text.isEmpty()) {
			return result;
		}
		for (String part : text.split(Pattern.quote(MATCH_SEPARATOR))) {
			int arrow = part.indexOf(" => ");
			if (arrow > 0) {
				result.putIfAbsent(part.substring(0, arrow), part.substring(arrow + 4));
			}
		}
		return result;
	}

	/** Parses "Name (rank, status, id=1, speciesId=2); Name2 (...)" into match maps. */
	static List<Object> parseDescribedMatches(String text, String source) {
		List<Object> matches = new ArrayList<>();
		Matcher m = DESCRIBED_MATCH.matcher(text);
		int pos = 0;
		while (pos < text.length() && m.find(pos) && m.start() == pos) {
			Map<String, Object> optMatch = new LinkedHashMap<>();
			optMatch.put("name", m.group(1));
			optMatch.put("rank", emptyToNull(m.group(2)));
			optMatch.put("status", emptyToNull(m.group(3)));
			optMatch.put("position", null);
			optMatch.put("group_name", null);
			optMatch.put("hierarchy", null);
			optMatch.put("id", toLong(m.group(4)));
			optMatch.put("source", source);
			optMatch.put("acceptedId", null);
			optMatch.put("speciesId", toLong(m.group(5)));
			matches.add(optMatch);
			pos = m.end();
		}
		if (pos < text.length()) {
			logger.warn("Could not parse match description from position {}: '{}'", pos, text);
		}
		return matches;
	}

	/** "Lang: name (id=1,2) | ..." -> {"name#Lang": 1} (first id) */
	static Map<String, Long> parseCnameMatches(String text) {
		Map<String, Long> result = new HashMap<>();
		if (text == null || text.isEmpty()) {
			return result;
		}
		for (String part : text.split(Pattern.quote(MATCH_SEPARATOR))) {
			Matcher m = CNAME_MATCH.matcher(part.trim());
			if (m.matches()) {
				result.putIfAbsent(m.group(2) + "#" + m.group(1), Long.valueOf(m.group(3).split(",")[0]));
			}
		}
		return result;
	}

	/** "Lang1: cn1, cn2 | Lang2: cn1" -> [lang, name] pairs. */
	static List<String[]> parseCommonNames(String cellText) {
		List<String[]> result = new ArrayList<>();
		for (String langGroup : cellText.split("\\s*\\|\\s*")) {
			if (langGroup.trim().isEmpty()) {
				continue;
			}
			String[] cnamedet = langGroup.split("\\s*:\\s*", 2);
			if (cnamedet.length < 2) {
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

	/** "Name [rank, id=1]" (current format) */
	private static final Pattern HIER_NODE = Pattern.compile("^(.*) \\[([^\\[\\],]*), id=(\\d+|-)\\]$");
	/** "{taxon_id=1, taxon_rank=root, taxon_name=Root}" (format of earlier files) */
	private static final Pattern HIER_NODE_MAP = Pattern
			.compile("^\\{taxon_id=([^,]*), taxon_rank=([^,]*), taxon_name=(.*)\\}$");

	/**
	 * "Animalia [kingdom, id=872] > Chordata [phylum, id=20218]" ->
	 * [{taxon_id: 872, taxon_rank: kingdom, taxon_name: Animalia}, ...]
	 */
	static List<Map<String, Object>> toHierarchy(String text) {
		if (text == null || text.isEmpty()) {
			return null;
		}
		List<Map<String, Object>> nodes = new ArrayList<>();
		for (String part : text.split(" > ")) {
			part = part.trim();
			if (part.isEmpty()) {
				continue;
			}
			Map<String, Object> node = new LinkedHashMap<>();
			Matcher m = HIER_NODE.matcher(part);
			Matcher mm = HIER_NODE_MAP.matcher(part);
			if (m.matches()) {
				node.put("taxon_id", toLong(m.group(3)));
				node.put("taxon_rank", emptyToNull(m.group(2)));
				node.put("taxon_name", m.group(1));
			} else if (mm.matches()) {
				node.put("taxon_id", toLong(mm.group(1)));
				node.put("taxon_rank", emptyToNull(mm.group(2)));
				node.put("taxon_name", mm.group(3));
			} else {
				node.put("taxon_name", part);
			}
			nodes.add(node);
		}
		return nodes;
	}

	/** "1,2" -> [1, 2]; non-numeric parts are kept as text. */
	static List<Object> toIdList(String text) {
		if (text == null || text.isEmpty()) {
			return null;
		}
		List<Object> ids = new ArrayList<>();
		for (String part : text.split("\\s*,\\s*")) {
			if (part.isEmpty()) {
				continue;
			}
			Long id = toLong(part);
			ids.add(id != null ? id : part);
		}
		return ids;
	}

	static Long toLong(String text) {
		if (text == null || text.isEmpty() || MISSING.equals(text)) {
			return null;
		}
		try {
			return Long.valueOf(text.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int metaInt(Map<String, String> meta, String key, Integer defaultValue) {
		String v = meta.get(key);
		if (v == null || v.isEmpty()) {
			if (defaultValue == null) {
				throw new IllegalArgumentException("Processed file is missing setting '" + key + "'");
			}
			return defaultValue;
		}
		try {
			return (int) Double.parseDouble(v); // tolerate "3.0" if a spreadsheet app re-typed it
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Processed file has an invalid '" + key + "': " + v);
		}
	}

	private static String emptyToNull(String s) {
		return s == null || s.isEmpty() ? null : s;
	}

	private static String text(Cell cell) {
		return cell == null ? "" : cell.toString();
	}
}