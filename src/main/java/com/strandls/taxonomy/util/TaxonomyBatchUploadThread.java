package com.strandls.taxonomy.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strandls.esmodule.pojo.Breadcrumb;
import com.strandls.esmodule.pojo.TaxonomyBulkUpdateData;
import com.strandls.esmodule.pojo.TaxonomyBulkUpdateRequest;
import com.strandls.taxonomy.dao.AcceptedSynonymDao;
import com.strandls.taxonomy.dao.TaxonomyDefinitionDao;
import com.strandls.taxonomy.dao.TaxonomyRegistryDao;
import com.strandls.taxonomy.pojo.AcceptedSynonym;
import com.strandls.taxonomy.pojo.TaxonomyDefinition;
import com.strandls.taxonomy.pojo.TaxonomyRegistry;
import com.strandls.taxonomy.pojo.enumtype.TaxonomyPosition;
import com.strandls.taxonomy.pojo.enumtype.TaxonomyStatus;
import com.strandls.taxonomy.pojo.response.BatchUpload;
import com.strandls.taxonomy.pojo.response.TaxonomyRegistryResponse;
import com.strandls.taxonomy.service.CommonNameSerivce;
import com.strandls.taxonomy.service.exception.TaxonCreationException;
import com.strandls.taxonomy.service.exception.UnRecongnizedRankException;
import com.strandls.taxonomy.service.impl.TaxonomyESOperation;
import com.strandls.utility.ApiException;
import com.strandls.utility.controller.UtilityServiceApi;
import com.strandls.utility.pojo.ParsedName;

/**
 * Runs a confirmed taxonomy batch upload (creates, updates, synonyms, common
 * names, species-page requests and position changes) in the background, so the
 * HTTP request returns as soon as the job is started.
 *
 * Moved out of TaxonomyDefinitionServiceImpl.batchUpload; the processing logic
 * is unchanged. Progress and the final counts are written to the log.
 */
public class TaxonomyBatchUploadThread implements Runnable {

	private static final int BATCH_SIZE = 1000;

	private final Logger logger = LoggerFactory.getLogger(TaxonomyBatchUploadThread.class);

	private final Long userId;
	private final List<BatchUpload> confirmRequests;

	private final TaxonomyDefinitionDao taxonomyDao;
	private final TaxonomyRegistryDao taxonomyRegistryDao;
	private final AcceptedSynonymDao acceptedSynonymDao;
	private final TaxonomyCache taxonomyCache;
	private final TaxonomyESOperation taxonomyESUpdate;
	private final TaxonomyEventProducer taxonomyEventProducer;
	private final CommonNameSerivce commonNameSerivce;
	private final UtilityServiceApi utilityServiceApi;

	public TaxonomyBatchUploadThread(Long userId, List<BatchUpload> confirmRequests,
			TaxonomyDefinitionDao taxonomyDao, TaxonomyRegistryDao taxonomyRegistryDao,
			AcceptedSynonymDao acceptedSynonymDao, TaxonomyCache taxonomyCache, TaxonomyESOperation taxonomyESUpdate,
			TaxonomyEventProducer taxonomyEventProducer, CommonNameSerivce commonNameSerivce,
			UtilityServiceApi utilityServiceApi) {
		this.userId = userId;
		// own copy: the request body list must not change under the running job
		this.confirmRequests = new ArrayList<>(confirmRequests);
		this.taxonomyDao = taxonomyDao;
		this.taxonomyRegistryDao = taxonomyRegistryDao;
		this.acceptedSynonymDao = acceptedSynonymDao;
		this.taxonomyCache = taxonomyCache;
		this.taxonomyESUpdate = taxonomyESUpdate;
		this.taxonomyEventProducer = taxonomyEventProducer;
		this.commonNameSerivce = commonNameSerivce;
		this.utilityServiceApi = utilityServiceApi;
	}

	/** Running totals across all batches. */
	private static class BatchCounters {
		long givenCreated = 0;
		long givenError = 0;
		long synCreated = 0;
		long synError = 0;

		String summary() {
			return givenCreated + "|" + givenError + "|" + synCreated + "|" + synError;
		}
	}

	@Override
	public void run() {
		long started = System.currentTimeMillis();
		logger.info("Taxonomy batch upload started by user {} for {} rows", userId, confirmRequests.size());
		BatchCounters counters = new BatchCounters();
		try {
			process(counters);
			logger.info("Taxonomy batch upload by user {} finished in {} ms; given created|error|syn created|error = {}",
					userId, System.currentTimeMillis() - started, counters.summary());
		} catch (Exception e) {
			logger.error("Taxonomy batch upload by user {} failed after {} ms (counts so far {}): {}", userId,
					System.currentTimeMillis() - started, counters.summary(), e.getMessage(), e);
		}
	}

	private void process(BatchCounters counters) {
		Map<ParsedName, Long> createMapping = new HashMap<>();
		Map<Long, String> positionHier = new HashMap<>();

		// 1. Divide into create and update requests
		List<BatchUpload> createRequests = new ArrayList<>();
		List<BatchUpload> updateRequests = new ArrayList<>();
		List<String> speciesPagecreate = new ArrayList<>();
		for (BatchUpload req : confirmRequests) {
			if (isCreateRequest(req)) {
				createRequests.add(req);
			} else {
				updateRequests.add(req);
			}
			if (req.getSpeciesId() != null && req.getSpeciesId().equals("CREATE")) {
				speciesPagecreate.add(req.getScientificName() + "|" + (long) Double.parseDouble(req.getTaxonId()));
			}
			if (req.getPositionHier() != null) {
				for (String h : req.getPositionHier()) {
					String[] parts = h.split("\\|");
					if (parts.length < 3) {
						continue; // or log / throw
					}

					String[] posParts = parts[2].split("#", 2);
					if (posParts.length < 2) {
						continue; // or log / throw
					}

					Long taxonId = Long.parseLong(parts[1].trim());
					positionHier.put(taxonId, posParts[1]);
				}
			}
		}

		// 2. Process each group in batches (creates first, so new taxa exist
		// before any updates run)
		for (List<BatchUpload> batch : partition(createRequests, BATCH_SIZE)) {
			processCreateBatch(batch, userId, counters, createMapping);
		}
		for (List<BatchUpload> batch : partition(updateRequests, BATCH_SIZE)) {
			processUpdateBatch(batch, userId, counters, createMapping);
		}
		List<List<String>> batches = new ArrayList<>();
		for (int i = 0; i < speciesPagecreate.size(); i += BATCH_SIZE) {
			batches.add(speciesPagecreate.subList(i, Math.min(i + BATCH_SIZE, speciesPagecreate.size())));
		}

		for (List<String> batch : batches) {
			processSpeciesPageBatch(batch, userId, counters);
		}

		List<Map.Entry<Long, String>> entries = new ArrayList<>(positionHier.entrySet());
		List<Map<Long, String>> positionbatches = new ArrayList<>();

		for (int i = 0; i < entries.size(); i += BATCH_SIZE) {
			Map<Long, String> batch = new LinkedHashMap<>();
			for (Map.Entry<Long, String> e : entries.subList(i, Math.min(i + BATCH_SIZE, entries.size()))) {
				batch.put(e.getKey(), e.getValue());
			}
			positionbatches.add(batch);
		}

		for (Map<Long, String> positionbatch : positionbatches) {
			processPositionHierBatch(positionbatch, userId);
		}

	}

	// ------------------------------------------------------------------
	// Batch processors
	// ------------------------------------------------------------------

	private void processCreateBatch(List<BatchUpload> batch, Long userId, BatchCounters counters,
			Map<ParsedName, Long> createMapping) {
		List<Long> taxonIds = new ArrayList<>();
		Map<String, Long> languageMapping = fetchLanguageMapping(batch);
		List<TaxonomyBulkUpdateData> updates = new ArrayList<>();
		List<Long> updateIds = new ArrayList<>();

		for (BatchUpload req : batch) {
			Long acceptedId = createHierarchy(req, userId, counters, taxonIds, createMapping);
			addSynonyms(req, acceptedId, userId, counters, taxonIds, updates, updateIds);
			addCommonNames(req, acceptedId, languageMapping);
			taxonIds.add(acceptedId);
		}

		pushToElastic(taxonIds);
	}

	private void processPositionHierBatch(Map<Long, String> positionbatch, Long userId) {
		List<Long> taxonIds = new ArrayList<>();
		List<TaxonomyBulkUpdateData> updates = new ArrayList<>();
		List<Long> updateIds = new ArrayList<>();

		List<TaxonomyDefinition> taxa = taxonomyDao.fetchByListOfIds(new ArrayList<>(positionbatch.keySet()));

		for (TaxonomyDefinition t : taxa) {
			t.setPosition(positionbatch.get(t.getId()));
			t = update(t);
			TaxonomyBulkUpdateData update = new TaxonomyBulkUpdateData();
			update.setPosition(t.getPosition());
			update.setTargetId(t.getId());
			updates.add(update);
			taxonIds.add(t.getId());
		}

		pushToElastic(taxonIds);

		if (!updates.isEmpty()) {
			TaxonomyBulkUpdateRequest taxonomyData = new TaxonomyBulkUpdateRequest();
			taxonomyData.setRecoIds(updateIds);
			taxonomyData.setUpdates(updates);
			taxonomyEventProducer.sendTaxonomyUpdate(taxonomyData, true, true, false);
		}
	}

	private void processUpdateBatch(List<BatchUpload> batch, Long userId, BatchCounters counters,
			Map<ParsedName, Long> createMapping) {
		List<Long> taxonIds = new ArrayList<>();
		List<TaxonomyBulkUpdateData> updates = new ArrayList<>();
		List<Long> updateIds = new ArrayList<>();
		Map<String, Long> languageMapping = fetchLanguageMapping(batch);

		for (BatchUpload req : batch) {
			Long acceptedId = applyUpdate(req, updates, updateIds, taxonIds, createMapping, userId);
			if (acceptedId == null) {
				continue; // malformed entry, skipped (same as original)
			}
			addSynonyms(req, acceptedId, userId, counters, taxonIds, updates, updateIds);
			addCommonNames(req, acceptedId, languageMapping);
			taxonIds.add(acceptedId);
		}

		pushToElastic(taxonIds);

		if (!updates.isEmpty()) {
			TaxonomyBulkUpdateRequest taxonomyData = new TaxonomyBulkUpdateRequest();
			taxonomyData.setRecoIds(updateIds);
			taxonomyData.setUpdates(updates);
			taxonomyEventProducer.sendTaxonomyUpdate(taxonomyData, true, true, false);
		}
	}

	// ------------------------------------------------------------------
	// Per-request logic
	// ------------------------------------------------------------------

	/**
	 * Copies the Target* source/match values from the Given Names sheet onto a
	 * taxon. Only values that were filled in are applied, so blank cells never
	 * clear what is already stored.
	 *
	 * @return true if anything changed (caller should then save the taxon)
	 */
	private boolean applyTargetSource(TaxonomyDefinition taxon, BatchUpload req) {
		boolean changed = false;
		if (req.getTargetViaDatasource() != null
				&& !req.getTargetViaDatasource().equals(taxon.getViaDatasource())) {
			taxon.setViaDatasource(req.getTargetViaDatasource());
			changed = true;
		}
		if (req.getTargetNameSourceId() != null && !req.getTargetNameSourceId().equals(taxon.getNameSourceId())) {
			taxon.setNameSourceId(req.getTargetNameSourceId());
			changed = true;
		}
		if (req.getTargetMatchId() != null && !req.getTargetMatchId().equals(taxon.getMatchId())) {
			taxon.setMatchId(req.getTargetMatchId());
			changed = true;
		}
		if (req.getTargetMatchDatabaseName() != null
				&& !req.getTargetMatchDatabaseName().equals(taxon.getMatchDatabaseName())) {
			taxon.setMatchDatabaseName(req.getTargetMatchDatabaseName());
			changed = true;
		}
		return changed;
	}

	private boolean isCreateRequest(BatchUpload req) {
		return req.getTaxonId() == null || "null".equals(req.getTaxonId());
	}

	private void processSpeciesPageBatch(List<String> batch, Long userId, BatchCounters counters) {

		if (!batch.isEmpty()) {
			taxonomyEventProducer.sendTaxonomyUpdate(batch, false, true, false);
		}
	}

	/**
	 * Creates missing nodes of the hierarchy. Returns the last created taxon id.
	 */
	private Long createHierarchy(BatchUpload req, Long userId, BatchCounters counters, List<Long> taxonIds,
			Map<ParsedName, Long> createMapping) {
		StringBuilder path = new StringBuilder();
		Long acceptedId = null;

		TaxonomyStatus status = TaxonomyStatus.fromValue(req.getStatus());
		TaxonomyPosition position = TaxonomyPosition.fromValue(req.getPosition());

		if (status.equals(TaxonomyStatus.ACCEPTED)) {

			// The last entry of the hierarchy is the given name itself; only it gets the
			// Target* source/match values, not the parent taxa created along the way.
			String[] hierarchyEntries = req.getHierarchy().split(";");
			int givenNameIndex = -1;
			for (int i = hierarchyEntries.length - 1; i >= 0; i--) {
				if (!hierarchyEntries[i].isBlank()) {
					givenNameIndex = i;
					break;
				}
			}

			for (int entryIndex = 0; entryIndex < hierarchyEntries.length; entryIndex++) {
				String entry = hierarchyEntries[entryIndex];
				if (entry.isBlank()) {
					continue;
				}
				boolean isGivenName = entryIndex == givenNameIndex;

				String[] rankAndRest = entry.split(":", 2);
				if (rankAndRest.length < 2) {
					continue; // malformed entry, skip
				}

				String rank = rankAndRest[0];
				String[] nameAndId = rankAndRest[1].split("#", 2);
				String name = nameAndId[0];
				String id = nameAndId.length > 1 ? nameAndId[1] : null;

				if (id != null && !id.isBlank()) {
					path.append(".").append(id);
					continue;
				}

				try {
					ParsedName parsedName = taxonomyCache.getName(rank, name);
					if (createMapping.containsKey(parsedName)) {
						path.append(".").append(createMapping.get(parsedName));
						continue;
					}
					TaxonomyDefinition taxonomyDefinition = taxonomyDao.createTaxonomyDefiniiton(parsedName, rank,
							status, position, isGivenName ? req.getTargetViaDatasource() : null,
							isGivenName ? req.getTargetNameSourceId() : null, userId);
					if (isGivenName && applyTargetSource(taxonomyDefinition, req)) {
						taxonomyDefinition = update(taxonomyDefinition);
					}
					counters.givenCreated++;

					Long taxonId = taxonomyDefinition.getId();
					acceptedId = taxonId;
					taxonIds.add(taxonId);

					path.append(".").append(taxonId);
					taxonomyRegistryDao.createRegistry(null, path.toString().substring(1), rank, taxonId, userId, null);

					createMapping.put(parsedName, taxonId);

				} catch (TaxonCreationException | UnRecongnizedRankException e) {
					e.printStackTrace();
					counters.givenError++;
				}
			}
		} else {
			ParsedName parsedName;
			try {
				parsedName = taxonomyCache.getName(req.getRank(), req.getScientificName());
				TaxonomyDefinition taxonomyDefinition = taxonomyDao.createTaxonomyDefiniiton(parsedName, req.getRank(),
						status, position, req.getTargetViaDatasource(), req.getTargetNameSourceId(), userId);
				if (applyTargetSource(taxonomyDefinition, req)) {
					taxonomyDefinition = update(taxonomyDefinition);
				}
				acceptedSynonymDao.createAcceptedSynonym((long) Double.parseDouble(req.getAcceptedId()),
						taxonomyDefinition.getId());
				acceptedId = taxonomyDefinition.getId();
			} catch (UnRecongnizedRankException | TaxonCreationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return acceptedId;
	}

	/**
	 * Applies position / name changes. Returns the taxon id, or null if the request
	 * is malformed.
	 * 
	 * @param createMapping
	 * @param userId
	 */
	private Long applyUpdate(BatchUpload req, List<TaxonomyBulkUpdateData> updates, List<Long> updateIds,
			List<Long> taxonIds, Map<ParsedName, Long> createMapping, Long userId) {
		Long acceptedId = (long) Double.parseDouble(req.getTaxonId());

		System.out.println(req.getHierarchy());
		String h = req.getHierarchy();
		if (h != null && !h.isBlank() && !"null".equals(h)) {

			String[] entries = req.getHierarchy().split(";");
			String last = entries[entries.length - 1];
			if (last.split(":", 2).length < 2) {
				return null; // malformed entry, skip
			}
		}

		TaxonomyDefinition taxonomyDefinition = findById(acceptedId);

		TaxonomyBulkUpdateData update = new TaxonomyBulkUpdateData();

		if (req.getPosition().contains("#")) {
			taxonomyDefinition.setPosition(req.getPosition().split("#")[1]);
			taxonomyDefinition = update(taxonomyDefinition);
			update.setPosition(taxonomyDefinition.getPosition());
			update.setTargetId(acceptedId);
		}

		if (req.getAcceptedId() != null && req.getAcceptedId().contains("#")) {
			Long prevAcceptedId = (long) Double.parseDouble(req.getAcceptedId().split("#")[0]);
			Long newTaxonId = (long) Double.parseDouble(req.getAcceptedId().split("#")[1]);
			AcceptedSynonym acceptedSynonym = acceptedSynonymDao.findByAccpetedIdSynonymId(prevAcceptedId, acceptedId);
			acceptedSynonym.setAcceptedId(newTaxonId);
			acceptedSynonymDao.update(acceptedSynonym);

			taxonIds.add(prevAcceptedId);
			taxonIds.add(newTaxonId);

			update.setTargetId(acceptedId);
			// update.setTransferSynonymIds(synonymIds);
			update.setNewId(newTaxonId);
			List<TaxonomyRegistryResponse> hierar = taxonomyRegistryDao.getPathToRoot(newTaxonId, null);
			List<Breadcrumb> breadCrumbs = new ArrayList<>();
			for (TaxonomyRegistryResponse crumb : hierar) {
				Breadcrumb breadCrumb = new Breadcrumb();
				breadCrumb.setTaxonId(Long.parseLong(crumb.getId()));
				breadCrumb.setTaxonName(crumb.getName());
				breadCrumb.setTaxonRank(crumb.getRank());
				breadCrumbs.add(breadCrumb);
			}
			update.setAcceptedBreadCrumbs(breadCrumbs);
		}

		else if (req.getStatus().contains("#")) {
			switch (TaxonomyStatus.fromValue(req.getStatus().split("#")[1])) {
			// Status is changing from synonym to accepted.
			case ACCEPTED:
				// status is changing from accepted to synonym
				Long taxonId = (long) 0;

				List<AcceptedSynonym> acceptedSynonyms = acceptedSynonymDao.findBySynonymId(acceptedId);
				for (AcceptedSynonym acceptedSynonym : acceptedSynonyms) {
					if (taxonId == 0) {
						taxonId = acceptedSynonym.getAcceptedId();
					}
					acceptedSynonymDao.delete(acceptedSynonym);
				}

				List<TaxonomyRegistryResponse> hierar = taxonomyRegistryDao.getPathToRoot(taxonId, null);
				StringBuilder path = new StringBuilder();

				String lastRank = null;

				for (String entry : req.getHierarchy().split(";")) {
					if (entry.isBlank()) {
						continue;
					}

					String[] rankAndRest = entry.split(":", 2);
					if (rankAndRest.length < 2) {
						continue; // malformed entry, skip
					}

					String rank = rankAndRest[0];
					lastRank = rank;
					String[] nameAndId = rankAndRest[1].split("#", 2);
					String name = nameAndId[0];
					String id = nameAndId.length > 1 ? nameAndId[1] : null;

					if (id != null && !id.isBlank()) {
						path.append(".").append(id);
						String taxonPath = taxonomyRegistryDao.findbyTaxonomyId(Long.parseLong(id), null).getPath();
						if (req.getUpdateHier().containsKey(id)) {
							List<Long> acceptedIds = taxonomyDao.getAllChildren(Long.parseLong(id));
							taxonIds.addAll(acceptedIds);
							taxonIds.addAll(acceptedSynonymDao.findSynonymIdsByAcceptedIds(acceptedIds));
							taxonomyDao.updatePath(path.toString().substring(1), taxonPath);
						}
						continue;
					}

					try {
						ParsedName parsedName = taxonomyCache.getName(rank, name);
						if (createMapping.containsKey(parsedName)) {
							path.append(".").append(createMapping.get(parsedName));
							continue;
						}
						TaxonomyDefinition htaxonomyDefinition = taxonomyDao.createTaxonomyDefiniiton(parsedName, rank,
								TaxonomyStatus.ACCEPTED, TaxonomyPosition.RAW, null, null, userId);

						taxonIds.add(htaxonomyDefinition.getId());

						path.append(".").append(htaxonomyDefinition.getId());
						taxonomyRegistryDao.createRegistry(null, path.toString().substring(1), rank, taxonId, userId,
								null);

						createMapping.put(parsedName, htaxonomyDefinition.getId());

					} catch (TaxonCreationException | UnRecongnizedRankException e) {
						e.printStackTrace();
					}
				}

				taxonomyDefinition.setStatus(TaxonomyStatus.ACCEPTED.name());
				taxonomyDefinition = update(taxonomyDefinition);

				taxonomyRegistryDao.createRegistry(null, path.toString().substring(1), lastRank, acceptedId, userId,
						null);

				hierar = taxonomyRegistryDao.getPathToRoot(acceptedId, null);
				List<Breadcrumb> breadCrumbs = new ArrayList<>();
				for (TaxonomyRegistryResponse crumb : hierar) {
					Breadcrumb breadCrumb = new Breadcrumb();
					breadCrumb.setTaxonId(Long.parseLong(crumb.getId()));
					breadCrumb.setTaxonName(crumb.getName());
					breadCrumb.setTaxonRank(crumb.getRank());
					breadCrumbs.add(breadCrumb);
				}
				taxonIds.add(acceptedId);
				update.setTargetId(acceptedId);
				update.setStatus(taxonomyDefinition.getStatus());
				update.setAcceptedBreadCrumbs(breadCrumbs);

				break;
			case SYNONYM:
				Long newTaxonId = (long) Double.parseDouble(req.getStatus().split("#")[2]);
				TaxonomyDefinition acceptedTaxonomy = taxonomyDao.findById(newTaxonId);

				if (acceptedTaxonomy == null)
					throw new IllegalArgumentException("Could not find the accepted taxonomy with the Id you provided");

				taxonomyDefinition.setStatus(TaxonomyStatus.SYNONYM.name());

				List<Long> children = taxonomyDao.getAllChildren(acceptedId);
				if (children.size() > 1)
					throw new IllegalArgumentException(
							"This name cannot be converted to a synonym because it has child taxa");
				acceptedSynonyms = acceptedSynonymDao.findByAccepetdId(acceptedId);
				List<Long> synonymIds = new ArrayList<>();
				for (AcceptedSynonym acceptedSynonym : acceptedSynonyms) {
					taxonIds.add(acceptedSynonym.getSynonymId());
					synonymIds.add(acceptedSynonym.getSynonymId());
				}
				taxonIds.add(newTaxonId);

				TaxonomyRegistry oldTaxonomyRegistry = taxonomyRegistryDao.findbyTaxonomyId(acceptedId, null);

				AcceptedSynonym acceptedSynonym = new AcceptedSynonym();
				acceptedSynonym.setAcceptedId(newTaxonId);
				acceptedSynonym.setSynonymId(acceptedId);
				acceptedSynonym.setVersion(0L);
				acceptedSynonymDao.save(acceptedSynonym);

				if (oldTaxonomyRegistry != null) {

					taxonomyRegistryDao.delete(oldTaxonomyRegistry);
				}
				// Update the status for given taxon node.
				taxonomyDefinition = update(taxonomyDefinition);

				update.setTargetId(acceptedId);
				update.setStatus(taxonomyDefinition.getStatus());
				update.setTransferSynonymIds(synonymIds);
				update.setNewId(newTaxonId);
				hierar = taxonomyRegistryDao.getPathToRoot(newTaxonId, null);
				breadCrumbs = new ArrayList<>();
				for (TaxonomyRegistryResponse crumb : hierar) {
					Breadcrumb breadCrumb = new Breadcrumb();
					breadCrumb.setTaxonId(Long.parseLong(crumb.getId()));
					breadCrumb.setTaxonName(crumb.getName());
					breadCrumb.setTaxonRank(crumb.getRank());
					breadCrumbs.add(breadCrumb);
				}
				update.setAcceptedBreadCrumbs(breadCrumbs);
				break;
			}
		}

		if (req.getScientificName().contains("#")) {
			try {
				ParsedName parsedName = utilityServiceApi.getNameParsed(req.getScientificName().split("#")[1]);

				String canonicalName = parsedName.getCanonical().getFull();

				taxonomyDefinition.setName(parsedName.getVerbatim().trim());
				taxonomyDefinition.setNormalizedForm(parsedName.getNormalized());
				taxonomyDefinition.setCanonicalForm(canonicalName);
				taxonomyDefinition.setBinomialForm(TaxonomyUtil.getBinomialName(canonicalName));
				taxonomyDefinition
						.setItalicisedForm(TaxonomyUtil.getItalicisedForm(parsedName, taxonomyDefinition.getRank()));
				taxonomyDefinition.setAuthorYear(
						parsedName.getAuthorship() != null ? parsedName.getAuthorship().getVerbatim() : null);
				taxonomyDefinition = update(taxonomyDefinition);

				taxonIds = taxonomyDao.getAllChildren(acceptedId);
				taxonIds.addAll(acceptedSynonymDao.findSynonymIdsByAcceptedIds(taxonIds));

				update.setName(taxonomyDefinition.getName());
				update.setNormalizedName(taxonomyDefinition.getNormalizedForm());
				update.setCanonicalForm(taxonomyDefinition.getCanonicalForm());
				update.setBinomialForm(taxonomyDefinition.getBinomialForm());
				update.setItalicisedForm(taxonomyDefinition.getItalicisedForm());
				update.setTargetId(acceptedId);
			} catch (ApiException e) {
				e.printStackTrace();
			}
		}

		// Source / match values from the sheet (only the ones that were filled in)
		if (applyTargetSource(taxonomyDefinition, req)) {
			taxonomyDefinition = update(taxonomyDefinition);
		}

		updates.add(update);
		updateIds.add(acceptedId);
		taxonIds.add(acceptedId);

		return acceptedId;
	}

	private void addSynonyms(BatchUpload req, Long acceptedId, Long userId, BatchCounters counters, List<Long> taxonIds,
			List<TaxonomyBulkUpdateData> updates, List<Long> updateIds) {

		for (String syn : req.getSynonyms()) {
			String[] parts = syn.split("\\|");

			if (parts[1].equals("CREATE")) {
				ParsedName synonymParsedName;

				// If any of these exceptions occur, skip the synonym.
				try {
					synonymParsedName = taxonomyCache.getName(parts[4], parts[0]);
					TaxonomyUtil.getRankForSynonym(synonymParsedName, parts[4]);
				} catch (UnRecongnizedRankException e) {
					continue;
				}

				try {
					TaxonomyDefinition taxonomyDefinition = taxonomyDao.createTaxonomyDefiniiton(synonymParsedName,
							parts[4], TaxonomyStatus.SYNONYM, TaxonomyPosition.RAW, null, null, userId);
					acceptedSynonymDao.createAcceptedSynonym(acceptedId, taxonomyDefinition.getId());
					taxonIds.add(taxonomyDefinition.getId());
					counters.synCreated++;
				} catch (TaxonCreationException e) {
					counters.synError++;
				}
			} else {
				if (parts[3].contains("#")) {
					Long taxonId = (long) Double.parseDouble(parts[1]);
					TaxonomyDefinition taxonomyDefinition = taxonomyDao.findById(taxonId);
					taxonomyDefinition.setPosition(parts[3].split("#")[1]);
					taxonomyDefinition = update(taxonomyDefinition);
					TaxonomyBulkUpdateData update = new TaxonomyBulkUpdateData();
					update.setPosition(taxonomyDefinition.getPosition());
					update.setTargetId(taxonId);
					taxonIds.add(taxonId);
				}
				if (parts[2].equals("SYNONYM") && Double.parseDouble(parts[5]) != acceptedId) {
					Long prevAcceptedId = (long) Double.parseDouble(parts[5]);
					Long newTaxonId = acceptedId;
					Long taxonId = (long) Double.parseDouble(parts[1]);
					AcceptedSynonym acceptedSynonym = acceptedSynonymDao.findByAccpetedIdSynonymId(prevAcceptedId,
							taxonId);
					acceptedSynonym.setAcceptedId(newTaxonId);
					acceptedSynonymDao.update(acceptedSynonym);

					taxonIds.add(prevAcceptedId);
					taxonIds.add(newTaxonId);

					TaxonomyBulkUpdateData update = new TaxonomyBulkUpdateData();

					update.setTargetId(taxonId);
					// update.setTransferSynonymIds(synonymIds);
					update.setNewId(newTaxonId);
					List<TaxonomyRegistryResponse> hierar = taxonomyRegistryDao.getPathToRoot(newTaxonId, null);
					List<Breadcrumb> breadCrumbs = new ArrayList<>();
					for (TaxonomyRegistryResponse crumb : hierar) {
						Breadcrumb breadCrumb = new Breadcrumb();
						breadCrumb.setTaxonId(Long.parseLong(crumb.getId()));
						breadCrumb.setTaxonName(crumb.getName());
						breadCrumb.setTaxonRank(crumb.getRank());
						breadCrumbs.add(breadCrumb);
					}
					update.setAcceptedBreadCrumbs(breadCrumbs);

					updates.add(update);
					updateIds.add(taxonId);
					taxonIds.add(taxonId);
				} else if (!parts[2].equals("SYNONYM")) {
					Long newTaxonId = acceptedId;
					Long taxonId = (long) Double.parseDouble(parts[1]);
					TaxonomyDefinition taxonomyDefinition = taxonomyDao.findById(taxonId);
					TaxonomyDefinition acceptedTaxonomy = taxonomyDao.findById(newTaxonId);

					if (acceptedTaxonomy == null)
						throw new IllegalArgumentException(
								"Could not find the accepted taxonomy with the Id you provided");

					taxonomyDefinition.setStatus(TaxonomyStatus.SYNONYM.name());

					/*
					 * taxonIds = taxonomyDao.getAllChildren(taxonId); if (taxonIds.size() > 1)
					 * throw new IllegalArgumentException(
					 * "This name cannot be converted to a synonym because it has child taxa");
					 */
					List<AcceptedSynonym> acceptedSynonyms = acceptedSynonymDao.findByAccepetdId(taxonId);
					List<Long> synonymIds = new ArrayList<>();
					for (AcceptedSynonym acceptedSynonym : acceptedSynonyms) {
						taxonIds.add(acceptedSynonym.getSynonymId());
						synonymIds.add(acceptedSynonym.getSynonymId());
					}
					taxonIds.add(newTaxonId);

					// Make relevant update to the database.
					TaxonomyRegistry oldTaxonomyRegistry = taxonomyRegistryDao.findbyTaxonomyId(taxonId, null);

					AcceptedSynonym acceptedSynonym = new AcceptedSynonym();
					acceptedSynonym.setAcceptedId(newTaxonId);
					acceptedSynonym.setSynonymId(taxonId);
					acceptedSynonym.setVersion(0L);
					acceptedSynonymDao.save(acceptedSynonym);

					if (oldTaxonomyRegistry != null) {

						taxonomyRegistryDao.delete(oldTaxonomyRegistry);
					}

					// Update the status for given taxon node.
					taxonomyDefinition = update(taxonomyDefinition);

					TaxonomyBulkUpdateData update = new TaxonomyBulkUpdateData();

					update.setTargetId(taxonId);
					update.setStatus(taxonomyDefinition.getStatus());
					update.setTransferSynonymIds(synonymIds);
					update.setNewId(newTaxonId);
					List<TaxonomyRegistryResponse> hierar = taxonomyRegistryDao.getPathToRoot(newTaxonId, null);
					List<Breadcrumb> breadCrumbs = new ArrayList<>();
					for (TaxonomyRegistryResponse crumb : hierar) {
						Breadcrumb breadCrumb = new Breadcrumb();
						breadCrumb.setTaxonId(Long.parseLong(crumb.getId()));
						breadCrumb.setTaxonName(crumb.getName());
						breadCrumb.setTaxonRank(crumb.getRank());
						breadCrumbs.add(breadCrumb);
					}
					update.setAcceptedBreadCrumbs(breadCrumbs);
					updates.add(update);
					updateIds.add(taxonId);
					taxonIds.add(taxonId);
				}
			}
		}
	}

	private void addCommonNames(BatchUpload req, Long acceptedId, Map<String, Long> languageMapping) {
		Map<Long, String[]> languageIdToCommonNames = new HashMap<>();

		for (String cn : req.getCommonNames()) {
			String[] parts = cn.split("\\|");
			String name = parts[0];
			String languageName = parts.length > 2 ? parts[2] : null;

			if (languageName == null || languageName.isBlank()) {
				continue;
			}

			Long languageId = languageMapping.get(languageName);
			if (languageId == null) {
				System.out.println("No matching language found for: " + languageName);
				continue;
			}

			languageIdToCommonNames.merge(languageId, new String[] { name }, (existing, added) -> {
				String[] merged = Arrays.copyOf(existing, existing.length + 1);
				merged[existing.length] = added[0];
				return merged;
			});
		}

		commonNameSerivce.addCommonNames(acceptedId, languageIdToCommonNames, null);
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	/** One language lookup per batch instead of one per request. */
	private Map<String, Long> fetchLanguageMapping(List<BatchUpload> batch) {
		Set<String> languages = new LinkedHashSet<>();
		for (BatchUpload req : batch) {
			for (String cn : req.getCommonNames()) {
				String[] parts = cn.split("\\|");
				if (parts.length > 2 && !parts[2].isBlank()) {
					languages.add(parts[2]);
				}
			}
		}
		if (languages.isEmpty()) {
			return new HashMap<>();
		}
		return taxonomyDao.fetchByListOfNames(new ArrayList<>(languages));
	}

	/** Drops nulls and duplicates before pushing to Elasticsearch. */
	private void pushToElastic(List<Long> taxonIds) {
		List<Long> ids = new ArrayList<>(new LinkedHashSet<>(taxonIds));
		ids.removeIf(Objects::isNull);
		if (!ids.isEmpty()) {
			taxonomyESUpdate.pushToElastic(ids);
		}
	}

	private static <T> List<List<T>> partition(List<T> list, int size) {
		List<List<T>> batches = new ArrayList<>();
		for (int i = 0; i < list.size(); i += size) {
			batches.add(list.subList(i, Math.min(i + size, list.size())));
		}
		return batches;
	}

	// Same behaviour as AbstractService.update / findById in the service
	private TaxonomyDefinition update(TaxonomyDefinition taxonomyDefinition) {
		taxonomyDao.update(taxonomyDefinition);
		return taxonomyDefinition;
	}

	private TaxonomyDefinition findById(Long id) {
		return taxonomyDao.findById(id);
	}
}
