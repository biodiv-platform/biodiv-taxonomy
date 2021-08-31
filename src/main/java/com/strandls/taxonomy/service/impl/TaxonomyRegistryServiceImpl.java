package com.strandls.taxonomy.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.strandls.taxonomy.dao.AcceptedSynonymDao;
import com.strandls.taxonomy.dao.RankDao;
import com.strandls.taxonomy.dao.TaxonomyDefinitionDao;
import com.strandls.taxonomy.dao.TaxonomyRegistryDao;
import com.strandls.taxonomy.pojo.AcceptedSynonym;
import com.strandls.taxonomy.pojo.Rank;
import com.strandls.taxonomy.pojo.TaxonomyDefinition;
import com.strandls.taxonomy.pojo.TaxonomyRegistry;
import com.strandls.taxonomy.pojo.enumtype.TaxonomyPosition;
import com.strandls.taxonomy.pojo.enumtype.TaxonomyStatus;
import com.strandls.taxonomy.pojo.response.BreadCrumb;
import com.strandls.taxonomy.pojo.response.TaxonRelation;
import com.strandls.taxonomy.pojo.response.TaxonTree;
import com.strandls.taxonomy.pojo.response.TaxonomyRegistryResponse;
import com.strandls.taxonomy.service.TaxonomyRegistryService;
import com.strandls.taxonomy.service.exception.TaxonCreationException;
import com.strandls.taxonomy.service.exception.UnRecongnizedRankException;
import com.strandls.taxonomy.util.AbstractService;
import com.strandls.taxonomy.util.TaxonomyUtil;
import com.strandls.utility.ApiException;

public class TaxonomyRegistryServiceImpl extends AbstractService<TaxonomyRegistry> implements TaxonomyRegistryService {

	@Inject
	private TaxonomyRegistryDao taxonomyRegistryDao;

	@Inject
	private AcceptedSynonymDao acceptedSynonymDao;

	@Inject
	private TaxonomyDefinitionDao taxonomyDefinitionDao;

	@Inject
	private RankDao rankDao;

	private static final int BATCH_SIZE = 1000;
	private static final String STATUS = "status";
	private static final String POSITION = "position";

	@Inject
	public TaxonomyRegistryServiceImpl(TaxonomyRegistryDao dao) {
		super(dao);
	}

	@Override
	public List<BreadCrumb> fetchByTaxonomyId(Long id) {
		TaxonomyRegistry taxoRegistry = taxonomyRegistryDao.findbyTaxonomyId(id, null);
		if (taxoRegistry == null) {
			List<AcceptedSynonym> acceptedSynonyms = acceptedSynonymDao.findBySynonymId(id);
			if (acceptedSynonyms != null && !acceptedSynonyms.isEmpty())
				taxoRegistry = taxonomyRegistryDao.findbyTaxonomyId(acceptedSynonyms.get(0).getAcceptedId(), null);
		}
		if (taxoRegistry == null)
			return new ArrayList<>();

		String[] paths = taxoRegistry.getPath().split("\\.");
		List<Long> taxonIds = new ArrayList<>();
		for(String path : paths) {
			taxonIds.add(Long.parseLong(path));
		}
		List<BreadCrumb> breadCrumbs = new ArrayList<>();
		List<TaxonomyDefinition> breadCrumbLists = taxonomyDefinitionDao.breadCrumbSearch(taxonIds);
		for (TaxonomyDefinition td : breadCrumbLists) {
			BreadCrumb breadCrumb = new BreadCrumb(td.getId(), td.getNormalizedForm(), td.getRank());
			breadCrumbs.add(breadCrumb);
		}

		return breadCrumbs;
	}

	@Override
	public List<TaxonTree> fetchTaxonTrees(List<Long> taxonList) {
		List<TaxonTree> taxonTree = new ArrayList<>();

		for (Long taxon : taxonList) {
			List<Long> taxonPath = new ArrayList<>();
			List<BreadCrumb> breadCrumbs = fetchByTaxonomyId(taxon);
			for (BreadCrumb breadCrumb : breadCrumbs) {
				taxonPath.add(breadCrumb.getId());
			}
			taxonTree.add(new TaxonTree(taxon, taxonPath));
		}

		return taxonTree;
	}

	@Override
	public List<TaxonRelation> list(Long parent, String taxonIds, boolean expandTaxon, Long classificationId) {
		List<Long> taxonID;
		if (taxonIds == null || "".equals(taxonIds.trim())) {
			taxonID = null;
		} else {
			taxonID = new ArrayList<>();
			for (String taxon : taxonIds.split(","))
				taxonID.add(Long.parseLong(taxon));
		}
		List<String> ids = taxonomyRegistryDao.getPathToRoot(taxonID, classificationId);
		try {
			List<TaxonRelation> inputItems = taxonomyRegistryDao.list(parent, taxonID, expandTaxon, classificationId);

			if (expandTaxon) {
				if (taxonIds != null && inputItems != null) {
					return buildHierarchy(inputItems, ids);
				}
			}
			return inputItems;

		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * 
	 * @param items dummy
	 * @param ids
	 * @return dummy
	 */
	private List<TaxonRelation> buildHierarchy(List<TaxonRelation> items, List<String> ids) {

		Map<Long, TaxonRelation> idItemMap = new HashMap<Long, TaxonRelation>();
		for (TaxonRelation item : items) {
			item.setIds(ids);
			idItemMap.put(item.getId(), item);
		}

		List<TaxonRelation> result = new ArrayList<>();
		for (TaxonRelation item : items) {
			Long parentId = item.getParent();

			if (parentId == null || !idItemMap.containsKey(parentId)) {
				result.add(item);
			} else {
				TaxonRelation relation = idItemMap.get(parentId);
				relation.addChild(item);
			}
		}
		return result;
	}

	/**
	 * Code below from here on is only for the migration purpose It does the tree
	 * migration
	 * 
	 * @throws TaxonCreationException
	 * @throws ApiException
	 * @throws UnRecongnizedRankException
	 */
	@Override
	public Map<String, Object> snapWorkingNames()
			throws CloneNotSupportedException, UnRecongnizedRankException, ApiException, TaxonCreationException {
		String countQueryString = "select id from taxonomy_definition where position = :position and status = :status and rank = :rank and is_deleted = false";
		String queryString = "from TaxonomyDefinition td where td.position = :position and td.status = :status and rank = :rank and isDeleted = false order by id";

		Map<String, Object> parameters = new HashMap<>();
		parameters.put(STATUS, TaxonomyStatus.ACCEPTED.name());
		parameters.put(POSITION, TaxonomyPosition.WORKING.name());

		return snapNames(queryString, parameters, countQueryString);
	}

	@Override
	public Map<String, Object> snapRawNames()
			throws CloneNotSupportedException, UnRecongnizedRankException, ApiException, TaxonCreationException {
		String countQueryString = "select id from taxonomy_definition where position = :position and status = :status and rank = :rank";
		String queryString = "from TaxonomyDefinition td where td.position = :position and td.status = :status and rank = :rank order by id";

		Map<String, Object> parameters = new HashMap<>();
		parameters.put(STATUS, TaxonomyStatus.ACCEPTED.name());
		parameters.put(POSITION, TaxonomyPosition.RAW.name());

		return snapNames(queryString, parameters, countQueryString);
	}

	private Map<String, Object> snapNames(String queryString, Map<String, Object> parameters, String countQueryString){

		Map<String, Object> duplicateWithSnapping = new HashMap<>();
		Map<String, Object> duplicateWithoutSnapping = new HashMap<>();
		Map<String, Object> missingEntry = new HashMap<>();
		Map<String, Object> requiredRankMissing = new HashMap<>();
		Map<String, Object> hierarchyMissmatched = new HashMap<>();
		Map<String, Object> duplicatesInHierarchy = new HashMap<>();

		Long duplicateWithSnappingCount = 0L;
		Long duplicateWithoutSnappingCount = 0L;
		Long requiredRankMissingCount = 0L;
		Long hierarchyMissmatchedCount = 0L;
		Long missingEntryCount = 0L;
		Long duplicatesInHierarchyCount = 0L;

		Long defaultClassificationId = TaxonomyRegistryDao.getDefaultClassificationId();
		List<Rank> ranksBottomToTop = rankDao.getAllRank(false);

		// Snapping the name rank wise from top to bottom
		List<Rank> ranksTopToBottom = rankDao.getAllRank(true);
		for (Rank rankToSnap : ranksTopToBottom) {

			// Snapping the each of the taxonomy by its rank
			parameters.put("rank", rankToSnap.getName());
			Long nameCount = taxonomyDefinitionDao.getRowCount(countQueryString, parameters);

			for (int i = 0; i < nameCount; i += BATCH_SIZE) {
				int offset = i;
				int limit = BATCH_SIZE;
				limit = offset + limit > nameCount.intValue() ? nameCount.intValue() : limit;
				List<TaxonomyDefinition> definitions = taxonomyDefinitionDao.getByQueryString(queryString, parameters,
						limit, offset);

				for (TaxonomyDefinition definition : definitions) {
					Long userId = definition.getUploaderId();
					List<TaxonomyRegistry> taxonomyRegistrys = taxonomyRegistryDao
							.getSnappingCandidates(definition.getId());
					if (taxonomyRegistrys.isEmpty()) { // Missing the entry in taxonomy registry
						if (definition.getIsDeleted().booleanValue())
							continue;
						missingEntry.put(definition.getId().toString(), "Missing entry for the definition in taxonomy");
						missingEntryCount++;
						continue;
					}

					boolean isDefaultHierchy = defaultClassificationId
							.equals(taxonomyRegistrys.get(0).getClassificationId());
					TaxonomyRegistry candidateToSnap;
					int size = taxonomyRegistrys.size();
					if (isDefaultHierchy) {
						if (size > 2) {
							candidateToSnap = taxonomyRegistrys.get(0);

							// Reporting this because we are falling back to IBP hierarchy
							Map<String, Object> duplicateHierarchyStatus = new HashMap<String, Object>();
							duplicateHierarchyStatus.put("Snapping candidate status", "found");
							duplicateHierarchyStatus.put("Snapping candidate", candidateToSnap);
							duplicateHierarchyStatus.put("Snapping Hierarchy",
									taxonomyRegistryDao.getNameFromPath(candidateToSnap.getPath()));
							duplicateHierarchyStatus.put("duplicate hierarchy", taxonomyRegistrys.subList(1, size));
							duplicateWithSnapping.put(definition.getId().toString(), duplicateHierarchyStatus);
							duplicateWithSnappingCount++;
						} else {
							// This is perfect scenario where you got single candidate for snapping
							candidateToSnap = taxonomyRegistrys.get(size - 1);
						}
					} else if (size == 1) {
						// Only one decision we have here to snap
						candidateToSnap = taxonomyRegistrys.get(0);
					} else {
						// We do not have proper snapping candidate here. Report
						Map<String, Object> duplicateHierarchyStatus = new HashMap<String, Object>();
						duplicateHierarchyStatus.put("Snapping candidate status", "Not found");
						duplicateHierarchyStatus.put("duplicate hierarchy", taxonomyRegistrys);
						duplicateWithoutSnapping.put(definition.getId().toString(), duplicateHierarchyStatus);
						duplicateWithoutSnappingCount++;
						continue;
					}

					// Get Name details for the path
					List<TaxonomyRegistryResponse> nodeWithParents = taxonomyRegistryDao
							.getNameFromPath(candidateToSnap.getPath());
					Set<String> rankNames = new HashSet<String>();
					for (TaxonomyRegistryResponse r : nodeWithParents)
						rankNames.add(r.getRank());

					// We got the duplicate hierarchy here
					if (rankNames.size() != nodeWithParents.size()) {
						Map<String, Object> r = new HashMap<String, Object>();
						r.put("status", "Duplicates in the hierarchy");
						r.put("path", candidateToSnap.getPath());
						r.put("hierarchy", nodeWithParents);
						r.put("classification", candidateToSnap.getClassificationId());
						duplicatesInHierarchy.put(definition.getId().toString(), r);
						duplicatesInHierarchyCount++;
						continue;
					}

					StringBuilder newPath = new StringBuilder();
					if (!TaxonomyUtil.validateHierarchy(ranksBottomToTop, rankNames)) {
						if (TaxonomyPosition.CLEAN.name().equals(definition.getPosition())) {
							Map<String, Object> r = new HashMap<String, Object>();
							r.put("status", "Required rank missing in the hierarchy");
							r.put("path", candidateToSnap.getPath());
							r.put("hierarchy", nodeWithParents);
							r.put("classification", candidateToSnap.getClassificationId());
							requiredRankMissing.put(definition.getId().toString(), r);
							requiredRankMissingCount++;
							continue;
						} else {
							Double highestRank = TaxonomyUtil.getHighestInputRank(ranksBottomToTop, rankNames);
							Map<String, TaxonomyRegistryResponse> rankToRegistry = new HashMap<String, TaxonomyRegistryResponse>();

							for (TaxonomyRegistryResponse r : nodeWithParents) {
								rankToRegistry.put(r.getRank(), r);
							}

							Long parentId = 0L;

							for (Rank rank : ranksTopToBottom) {
								// Skipping these ranks,
								if (rank.getRankValue() > highestRank)
									break;
								if (rank.getIsRequired().booleanValue() && !rankNames.contains(rank.getName())) {
									TaxonomyPosition position = TaxonomyPosition.fromValue(definition.getPosition());
									TaxonomyDefinition notAssignedName = taxonomyRegistryDao
											.findChildWithNotAssigned(parentId, null);
									if (notAssignedName == null) {
										// Create not assigned name.
										notAssignedName = taxonomyDefinitionDao.createNotAssignedName(rank.getName(),
												position, userId);
									}
									parentId = notAssignedName.getId();
								} else if (!rank.getIsRequired().booleanValue()
										&& !rankNames.contains(rank.getName())) {
									continue;
								} else {
									parentId = Long.parseLong(rankToRegistry.get(rank.getName()).getId());
								}
								if (!"".equals(newPath.toString()))
									newPath.append(".");
								newPath.append(parentId);
							}
						}
					}

					// If there is no update to path then use existing one
					if (newPath.length() == 0) {
						newPath.append(candidateToSnap.getPath());
					} else {
						// This is the case when there is change in the path of choosen candidate
						// New node with not assigned value is used
						nodeWithParents = taxonomyRegistryDao.getNameFromPath(newPath.toString());
					}

					// Get the parent on which we are going to snap
					TaxonomyRegistry parentToSnapOn = taxonomyRegistryDao.getParentToSnapOn(newPath.toString());

					// Generate base path with respect to snapping parent
					StringBuilder path = new StringBuilder(parentToSnapOn.getPath());

					// Move till you get the correct parent on which you will snap.
					int index = 0;
					while (!nodeWithParents.get(index).getId()
							.equals(parentToSnapOn.getTaxonomyDefinationId().toString()))
						index++;

					// Moving for the node.
					index++;

					// Create registry for all the children below the node.
					while (index < nodeWithParents.size()) {
						TaxonomyRegistryResponse treeNode = nodeWithParents.get(index);
						path.append(".");
						path.append(treeNode.getId());
						String rank = treeNode.getRank();
						Long taxonId = Long.parseLong(treeNode.getId());
						taxonomyRegistryDao.createRegistry(defaultClassificationId, path.toString(), rank, taxonId,
								userId, definition.getUploadTime());
						index++;
					}

					if (isDefaultHierchy) {
						String defaultHierarchy = taxonomyRegistrys.get(0).getPath();
						if (!defaultHierarchy.equals(path.toString())) {
							Map<String, Object> hierarchyMissmatchedStatus = new HashMap<>();
							hierarchyMissmatchedStatus.put("Taxon Id", definition.getId());

							hierarchyMissmatchedStatus.put("IBP hierarchy path", defaultHierarchy);
							hierarchyMissmatchedStatus.put("IBP hierarchy name",
									taxonomyRegistryDao.getNameFromPath(defaultHierarchy));

							hierarchyMissmatchedStatus.put("Snapped hierarchy path", path);
							hierarchyMissmatchedStatus.put("Snapped hierarchy name",
									taxonomyRegistryDao.getNameFromPath(path.toString()));

							hierarchyMissmatchedStatus.put("Choosen hierarchy path", newPath.toString());
							hierarchyMissmatchedStatus.put("Choosen hierarchy name",
									taxonomyRegistryDao.getNameFromPath(newPath.toString()));
							hierarchyMissmatchedStatus.put("classification", candidateToSnap.getClassificationId());
							hierarchyMissmatched.put(definition.getId().toString(), hierarchyMissmatchedStatus);
							hierarchyMissmatchedCount++;
						}
					}
				}
			}
		}

		requiredRankMissing.put("requiredRankMissingCount", requiredRankMissingCount);
		missingEntry.put("missingEntryCount", missingEntryCount);
		duplicateWithSnapping.put("duplicateWithSnappingCount", duplicateWithSnappingCount);
		duplicateWithoutSnapping.put("duplicateWithoutSnappingCount", duplicateWithoutSnappingCount);
		hierarchyMissmatched.put("hierarchyMissmatchedCount", hierarchyMissmatchedCount);
		duplicatesInHierarchy.put("duplicatesInHierarchyCount", duplicatesInHierarchyCount);

		Map<String, Object> response = new HashMap<>();
		Long count = taxonomyDefinitionDao.getRowCount();
		response.put("totalCount", count);
		response.put("duplicateWithSnapping", duplicateWithSnapping);
		response.put("duplicateWithoutSnapping", duplicateWithoutSnapping);
		response.put("missingEntry", missingEntry);
		response.put("hierarchyMissmatched", hierarchyMissmatched);
		response.put("requiredRankMissing", requiredRankMissing);
		response.put("duplicatesInHierarchy", duplicatesInHierarchy);
		return response;
	}

	public Map<String, Object> migrateCleanName() throws CloneNotSupportedException {

		String countQueryString = "select id from taxonomy_definition where position = :position and status = :status";
		String queryString = "from TaxonomyDefinition td where td.position = :position and td.status = :status order by id";

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("status", TaxonomyStatus.ACCEPTED.name());

		// migrate the clean name on to the taxonomy tree.
		parameters.put("position", TaxonomyPosition.CLEAN.name());
		Long cleanNameCount = taxonomyDefinitionDao.getRowCount(countQueryString, parameters);

		List<Long> classificationIds = new ArrayList<>();
		classificationIds.add(TaxonomyRegistryDao.getDefaultClassificationId());
		Map<String, Object> missingNameInRegistry = new HashMap<>();
		Map<String, Object> missingNameInDefinition = new HashMap<>();
		Map<String, Object> pathAndHierarchyMissmatch = new HashMap<>();
		Map<String, Object> requiredRankMissing = new HashMap<>();

		int missingNameInRegistryCount = 0;
		int missingNameInDefinitionCount = 0;
		int pathAndHierarchyMissmatchCount = 0;
		int requiredRankMissingCount = 0;

		List<Rank> ranks = rankDao.getAllRank();
		/*
		 * classificationIds.add(821L); classificationIds.add(819L);
		 * classificationIds.add(818L); classificationIds.add(817L);
		 */

		// migrate in the batch of 1000
		for (int i = 0; i < cleanNameCount; i += BATCH_SIZE) {
			int offset = i;
			int limit = BATCH_SIZE;
			limit = offset + limit > cleanNameCount.intValue() ? cleanNameCount.intValue() : limit;
			List<TaxonomyDefinition> definitions = taxonomyDefinitionDao.getByQueryString(queryString, parameters,
					limit, offset);

			for (TaxonomyDefinition definition : definitions) {

				List<TaxonomyRegistry> taxonomyRegistrys = taxonomyRegistryDao.getOldHierarchy(definition.getId(),
						classificationIds);

				if (taxonomyRegistrys.isEmpty()) {
					if (definition.getIsDeleted().booleanValue())
						continue;
					Map<String, Object> r = new HashMap<>();
					r.put("status", "Name present but missing in the registry");
					r.put(definition.getId().toString(), definition.getName());
					missingNameInRegistry.put(definition.getId().toString(), r);
					missingNameInRegistryCount++;
					continue;
				} else {
					List<TaxonomyRegistryResponse> generatedHierarchyName = taxonomyRegistryDao
							.getPathToRootForOldHierarchy(definition.getId(),
									TaxonomyRegistryDao.getDefaultClassificationId());

					StringBuilder pathToRootIdsBuilder = new StringBuilder();

					for (TaxonomyRegistryResponse r : generatedHierarchyName) {
						pathToRootIdsBuilder.append(".");
						pathToRootIdsBuilder.append(r.getId());
					}
					String pathToRootIds = pathToRootIdsBuilder.substring(1);

					String path = taxonomyRegistrys.get(0).getPath();
					List<TaxonomyRegistryResponse> actualHierarchyName = taxonomyRegistryDao.getNameFromPath(path);

					// Check for the valid hierarchy - See if all the required ranks are present.
					Set<String> rankNames = new HashSet<>();
					for (TaxonomyRegistryResponse r : actualHierarchyName)
						rankNames.add(r.getRank());

					if (!pathToRootIds.equals(path)) {
						Map<String, Object> r = new HashMap<>();
						if (definition.getIsDeleted().booleanValue()) {
							r.put("status", "Name deleted but entry in the registry");
							r.put("actualPath", path);
							r.put("generatedPath", pathToRootIds);

							r.put("actualHierarchy", actualHierarchyName);
							r.put("generatedHierarchy", generatedHierarchyName);
							missingNameInDefinition.put(definition.getId().toString(), r);
							missingNameInDefinitionCount++;
						} else {
							r.put("status", "Missmatch between registry path and generated path");
							r.put("actualPath", path);
							r.put("generatedPath", pathToRootIds);

							r.put("actualHierarchy", actualHierarchyName);
							r.put("generatedHierarchy", generatedHierarchyName);
							pathAndHierarchyMissmatch.put(definition.getId().toString(), r);
							pathAndHierarchyMissmatchCount++;
						}
					}
					if (!TaxonomyUtil.validateHierarchy(ranks, rankNames)) {
						Map<String, Object> r = new HashMap<>();
						r.put("status", "Required rank missing in the hierarchy");
						r.put("actualPath", path);
						r.put("actualHierarchy", actualHierarchyName);
						requiredRankMissing.put(definition.getId().toString(), r);
						requiredRankMissingCount++;
					}
				}

				TaxonomyRegistry ibpHierarchy = taxonomyRegistrys.get(0).clone();
				save(ibpHierarchy);
			}

		}

		missingNameInDefinition.put("missingNameInDefinitionCount", missingNameInDefinitionCount);
		missingNameInRegistry.put("missingNameInRegistryCount", missingNameInRegistryCount);
		pathAndHierarchyMissmatch.put("pathAndHierarchyMissmatchCount", pathAndHierarchyMissmatchCount);
		requiredRankMissing.put("requiredRankMissingCount", requiredRankMissingCount);

		Map<String, Object> response = new HashMap<>();
		response.put("missingNameInRegistry", missingNameInRegistry);
		response.put("missingNameInDefinition", missingNameInDefinition);
		response.put("pathAndHierarchyMissmatch", pathAndHierarchyMissmatch);
		response.put("requiredRankMissing", requiredRankMissing);
		response.put("cleanNameCount", cleanNameCount);

		return response;

	}

}
