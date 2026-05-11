package com.strandls.taxonomy.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.strandls.taxonomy.dao.TaxonomyDefinitionDao;
import com.strandls.taxonomy.pojo.TaxonomyDefinition;
import com.strandls.taxonomy.pojo.enumtype.TaxonomyPosition;

public class TaxonomyBulkMappingThread implements Runnable {

	//private final Logger logger = LoggerFactory.getLogger(TaxonomyBulkMappingThread.class);

	private Boolean selectAll;
	private String bulkAction;
	private String bulkTaxonIds;
	private String bulkPosition;
	private TaxonomyDefinitionDao taxonomyDefinitionDao;

	public TaxonomyBulkMappingThread(Boolean selectAll, String bulkAction, String bulkTaxonIds, String bulkPosition,
			TaxonomyDefinitionDao taxonomyDefinitionDao) {
		super();
		this.selectAll = selectAll;
		this.bulkAction = bulkAction;
		this.bulkTaxonIds = bulkTaxonIds;
		this.bulkPosition = bulkPosition;
		this.taxonomyDefinitionDao = taxonomyDefinitionDao;
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

	}

	private void bulkPositionAction(List<TaxonomyDefinition> taxonList, TaxonomyPosition taxonomyPosition) {
		for (TaxonomyDefinition taxon : taxonList) {
			if (!taxonomyPosition.name().equals(taxon.getPosition())) {
				String oldPosition = taxon.getPosition();
				taxon.setPosition(taxonomyPosition.name());
				taxonomyDefinitionDao.update(taxon);

				/*
				 * String desc = "Taxon position updated  : " + oldPosition + "-->" +
				 * position.name();
				 * logActivity.logTaxonomyActivities(request.getHeader(HttpHeaders.AUTHORIZATION
				 * ), desc, taxonomyDefinition.getId(), taxonomyDefinition.getId(), "taxonomy",
				 * taxonomyDefinition.getId(), "Taxon position updated");
				 */
			}

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
		/*
		 * List<Long> obsIds = obsList.stream().map(item ->
		 * item.getId()).collect(Collectors.toList()); String observationList =
		 * StringUtils.join(obsIds, ','); ESBulkUploadThread updateThread = new
		 * ESBulkUploadThread(esUpdate, observationList); Thread esThreadUpdate = new
		 * Thread(updateThread); esThreadUpdate.start();
		 */
	}
}
