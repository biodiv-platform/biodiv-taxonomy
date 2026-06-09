package com.strandls.taxonomy.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.strandls.esmodule.controllers.EsServicesApi;
import com.strandls.esmodule.pojo.TaxonomyUpdateData;
import com.strandls.taxonomy.dao.AcceptedSynonymDao;
import com.strandls.taxonomy.dao.CommonNameDao;
import com.strandls.taxonomy.dao.TaxonomyDefinitionDao;
import com.strandls.taxonomy.dao.TaxonomyRegistryDao;
import com.strandls.taxonomy.pojo.AcceptedSynonym;
import com.strandls.taxonomy.pojo.TaxonomyDefinition;
import com.strandls.taxonomy.pojo.TaxonomyRegistry;
import com.strandls.taxonomy.pojo.enumtype.TaxonomyPosition;
import com.strandls.taxonomy.service.impl.TaxonomyESOperation;

import jakarta.inject.Inject;

public class TaxonomyBulkMappingThread implements Runnable {

	// private final Logger logger =
	// LoggerFactory.getLogger(TaxonomyBulkMappingThread.class);

	private Boolean selectAll;
	private String bulkAction;
	private String bulkTaxonIds;
	private String bulkPosition;
	private TaxonomyDefinitionDao taxonomyDefinitionDao;
	private TaxonomyRegistryDao taxonomyRegistryDao;
	private AcceptedSynonymDao acceptedSynonymDao;
	private CommonNameDao commonNameDao;
	private TaxonomyESOperation taxonomyESUpdate;
	private EsServicesApi esServicesApi;

	public TaxonomyBulkMappingThread(Boolean selectAll, String bulkAction, String bulkTaxonIds, String bulkPosition,
			TaxonomyDefinitionDao taxonomyDefinitionDao, AcceptedSynonymDao acceptedSynonymDao, CommonNameDao commonNameDao,
			TaxonomyESOperation taxonomyESUpdate, TaxonomyRegistryDao taxonomyRegistryDao, EsServicesApi esServicesApi) {
		super();
		this.selectAll = selectAll;
		this.bulkAction = bulkAction;
		this.bulkTaxonIds = bulkTaxonIds;
		this.bulkPosition = bulkPosition;
		this.taxonomyDefinitionDao = taxonomyDefinitionDao;
		this.acceptedSynonymDao = acceptedSynonymDao;
		this.commonNameDao = commonNameDao;
		this.taxonomyESUpdate = taxonomyESUpdate;
		this.taxonomyRegistryDao = taxonomyRegistryDao;
		this.esServicesApi = esServicesApi;
	}

	@Override
	public void run() {

		List<Long> taxonIds = new ArrayList<Long>();

		if (bulkTaxonIds != null && !bulkTaxonIds.isEmpty() && Boolean.FALSE.equals(selectAll)) {
			taxonIds.addAll(Arrays.stream(bulkTaxonIds.split(",")).map(Long::valueOf).collect(Collectors.toList()));
		}

		/*
		 * if (Boolean.TRUE.equals(selectAll)) { List<ShowSpeciesPage> specieList = new
		 * ArrayList<ShowSpeciesPage>();
		 * 
		 * try {
		 * 
		 * MapResponse result = esService.search(index, type, null, null, false, null,
		 * null, mapSearchQuery); List<MapDocument> documents = result.getDocuments();
		 * 
		 * for (MapDocument document : documents) { JsonNode rootNode =
		 * objectMapper.readTree(document.getDocument().toString()); ((ObjectNode)
		 * rootNode).remove("id"); ((ObjectNode) rootNode).replace("featured", null);
		 * ((ObjectNode) rootNode).replace("facts", null); ((ObjectNode)
		 * rootNode).replace("fieldData", null); JsonNode child = ((ObjectNode)
		 * rootNode).get("taxonomyDefinition"); ((ObjectNode)
		 * child).replace("defaultHierarchy", null);
		 * 
		 * try {
		 * 
		 * specieList.add(objectMapper.readValue(String.valueOf(rootNode),
		 * ShowSpeciesPage.class)); } catch (IOException e) {
		 * logger.error(e.getMessage()); } }
		 * 
		 * specieList.forEach(item -> { UserGroupObvFilterData ugFilterData = new
		 * UserGroupObvFilterData();
		 * ugFilterData.setObservationId(item.getSpecies().getId());
		 * list.add(ugFilterData); });
		 * 
		 * } catch (IOException | ApiException e) { e.printStackTrace();
		 * logger.error(e.getMessage()); }
		 * 
		 * }
		 */

		if (!bulkAction.isEmpty() && (bulkAction.contains("position"))) {
			List<TaxonomyDefinition> taxonDataList = new ArrayList<TaxonomyDefinition>();
			List<Long> taxIds = new ArrayList<Long>();
			if (bulkPosition != null && !bulkPosition.isEmpty()) {
				if (!taxonIds.isEmpty()) {
					taxonDataList = taxonomyDefinitionDao.fetchByListOfIds(taxonIds);

				}
				/*
				 * if (Boolean.TRUE.equals(selectAll)) { MapResponse result =
				 * esService.search(index, type, geoAggregationField, geoAggegationPrecision,
				 * onlyFilteredAggregation, termsAggregationField, geoShapeFilterField,
				 * mapSearchQuery); List<MapDocument> documents = result.getDocuments(); for
				 * (MapDocument document : documents) { ObservationListMinimalData data =
				 * objectMapper.readValue( String.valueOf(document.getDocument()),
				 * ObservationListMinimalData.class); obIds.add(data.getObservationId()); } }
				 */
				List<TaxonomyDefinition> TaxonList = new ArrayList<TaxonomyDefinition>();
				List<Long> TaxonIdList = new ArrayList<Long>();
				;
				Integer count = 0;

				if (Boolean.FALSE.equals(selectAll)) {
					while (count < taxonDataList.size()) {
						TaxonList.add(taxonDataList.get(count));

						if (TaxonList.size() >= 200) {
							bulkPositionAction(TaxonList, TaxonomyPosition.fromValue(bulkPosition));
							TaxonList.clear();
						}
						count++;
					}

					bulkPositionAction(TaxonList, TaxonomyPosition.fromValue(bulkPosition));
					TaxonList.clear();
				} /*
					 * else { while (count < obIds.size()) { ObsIdList.add(obIds.get(count)); if
					 * (ObsIdList.size() >= 200) {
					 * bulkSpeciesGroupAction(observationDao.fecthByListOfIds(ObsIdList), sGroupId);
					 * ObsIdList.clear(); } count++; }
					 * bulkSpeciesGroupAction(observationDao.fecthByListOfIds(ObsIdList), sGroupId);
					 * ObsIdList.clear(); }
					 */
			}
		}

		if (!bulkAction.isEmpty() && (bulkAction.contains("merge"))) {
			List<TaxonomyRegistry> taxonDataList = new ArrayList<>();
			List<Long> taxIds = new ArrayList<Long>();
			Long taxonId = taxonIds.remove(0); 
			if (!taxonIds.isEmpty()) {
				taxonDataList = taxonomyRegistryDao.fetchByListOfTaxonomyIds(taxonIds);

			}
			TaxonomyRegistry mergeRegistry = taxonomyRegistryDao.findbyTaxonomyId(taxonId, null);
			/*
			 * if (Boolean.TRUE.equals(selectAll)) { MapResponse result =
			 * esService.search(index, type, geoAggregationField, geoAggegationPrecision,
			 * onlyFilteredAggregation, termsAggregationField, geoShapeFilterField,
			 * mapSearchQuery); List<MapDocument> documents = result.getDocuments(); for
			 * (MapDocument document : documents) { ObservationListMinimalData data =
			 * objectMapper.readValue( String.valueOf(document.getDocument()),
			 * ObservationListMinimalData.class); obIds.add(data.getObservationId()); } }
			 */
			List<TaxonomyRegistry> TaxonList = new ArrayList<TaxonomyRegistry>();
			List<Long> TaxonIdList = new ArrayList<Long>();
			;
			Integer count = 0;

			if (Boolean.FALSE.equals(selectAll)) {
				while (count < taxonDataList.size()) {
					TaxonList.add(taxonDataList.get(count));

					if (TaxonList.size() >= 200) {
						bulkMergeAction(TaxonList, mergeRegistry.getPath(), taxonId);
						TaxonList.clear();
					}
					count++;
				}

				bulkMergeAction(TaxonList, mergeRegistry.getPath(), taxonId);
				TaxonList.clear();
			} /*
				 * else { while (count < obIds.size()) { ObsIdList.add(obIds.get(count)); if
				 * (ObsIdList.size() >= 200) {
				 * bulkSpeciesGroupAction(observationDao.fecthByListOfIds(ObsIdList), sGroupId);
				 * ObsIdList.clear(); } count++; }
				 * bulkSpeciesGroupAction(observationDao.fecthByListOfIds(ObsIdList), sGroupId);
				 * ObsIdList.clear(); }
				 */
		}

	}

	private void bulkPositionAction(List<TaxonomyDefinition> taxonList, TaxonomyPosition taxonomyPosition) {
		List<Long> taxonIds = new ArrayList<>();
		for (TaxonomyDefinition taxon : taxonList) {
			if (!taxonomyPosition.name().equals(taxon.getPosition())) {
				String oldPosition = taxon.getPosition();
				taxon.setPosition(taxonomyPosition.name());
				taxon = taxonomyDefinitionDao.update(taxon);

				/*
				 * String desc = "Taxon position updated  : " + oldPosition + "-->" +
				 * position.name();
				 * logActivity.logTaxonomyActivities(request.getHeader(HttpHeaders.AUTHORIZATION
				 * ), desc, taxonomyDefinition.getId(), taxonomyDefinition.getId(), "taxonomy",
				 * taxonomyDefinition.getId(), "Taxon position updated");
				 */
				if (taxon.getPosition().equals(taxonomyPosition.name())) {
					taxonIds.add(taxon.getId());
				}
			}
		}
		taxonomyESUpdate.pushToElastic(taxonIds);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		String timestamp = sdf.format(new Date());

		try {
			TaxonomyUpdateData taxonomyData = new TaxonomyUpdateData();
			taxonomyData.setTargetId(taxonList.get(0).getId());
			taxonomyData.setPosition(taxonomyPosition.name());
			taxonomyData.setTimestamp(timestamp);
			taxonomyData.setBulkIds(taxonIds);
			esServicesApi.updateAsync(taxonomyData);
		} catch (com.strandls.esmodule.ApiException e) {
			e.printStackTrace();
		}
	}
	
	private void bulkMergeAction(List<TaxonomyRegistry> taxonList, String path, Long taxonId) {
		List<Long> taxonIds = new ArrayList<>();
		List<Long> deleteTaxonIds = new ArrayList<>();
		for (TaxonomyRegistry taxon : taxonList) {
			taxonomyDefinitionDao.updatePath(path, taxon.getPath());
			acceptedSynonymDao.allSynonymTransfer(taxon.getTaxonomyDefinationId(), taxonId);
			commonNameDao.allCommonNameTransfer(taxon.getTaxonomyDefinationId(), taxonId);
			taxonIds.add(taxon.getTaxonomyDefinationId());
			taxonIds.addAll(taxonomyDefinitionDao.getAllChildren(taxon.getTaxonomyDefinationId()));
			List<AcceptedSynonym> acceptedSynonyms = acceptedSynonymDao.findByAccepetdId(taxonId);
			for (AcceptedSynonym acceptedSynonym : acceptedSynonyms) {
				taxonIds.add(acceptedSynonym.getSynonymId());
			}
			deleteTaxonIds.add(taxon.getTaxonomyDefinationId());
			taxonomyRegistryDao.delete(taxon);
			//taxonIds.addAll(taxonomyDefinitionDao.getAllChildren(taxon.getTaxonomyDefinationId()));
				
				
				/*
				 * String desc = "Taxon position updated  : " + oldPosition + "-->" +
				 * position.name();
				 * logActivity.logTaxonomyActivities(request.getHeader(HttpHeaders.AUTHORIZATION
				 * ), desc, taxonomyDefinition.getId(), taxonomyDefinition.getId(), "taxonomy",
				 * taxonomyDefinition.getId(), "Taxon position updated");
				 */

			/*
			 * List<Long> taxonIds = new ArrayList<>(); taxonIds.add(taxonId);
			 * List<AcceptedSynonym> acceptedSynonyms =
			 * acceptedSynonymDao.findByAccepetdId(taxonId); for (AcceptedSynonym
			 * acceptedSynonym : acceptedSynonyms) {
			 * taxonIds.add(acceptedSynonym.getSynonymId()); }
			 * 
			 * taxonomyESUpdate.pushToElastic(taxonIds);
			 */
		}
		taxonomyDefinitionDao.deleteByIds(deleteTaxonIds);
		taxonomyESUpdate.pushToElastic(taxonIds);
		/*
		 * List<Long> obsIds = obsList.stream().map(item ->
		 * item.getId()).collect(Collectors.toList()); String observationList =
		 * StringUtils.join(obsIds, ','); ESBulkUploadThread updateThread = new
		 * ESBulkUploadThread(esUpdate, observationList); Thread esThreadUpdate = new
		 * Thread(updateThread); esThreadUpdate.start();
		 */
	}
}
