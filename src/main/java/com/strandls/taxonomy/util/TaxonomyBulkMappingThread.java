package com.strandls.taxonomy.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.strandls.esmodule.ApiException;
import com.strandls.esmodule.controllers.EsServicesApi;
import com.strandls.esmodule.pojo.Breadcrumb;
import com.strandls.esmodule.pojo.TaxonomyUpdateData;
import com.strandls.taxonomy.dao.AcceptedSynonymDao;
import com.strandls.taxonomy.dao.CommonNameDao;
import com.strandls.taxonomy.dao.TaxonomyDefinitionDao;
import com.strandls.taxonomy.dao.TaxonomyRegistryDao;
import com.strandls.taxonomy.pojo.AcceptedSynonym;
import com.strandls.taxonomy.pojo.TaxonomyDefinition;
import com.strandls.taxonomy.pojo.TaxonomyRegistry;
import com.strandls.taxonomy.pojo.enumtype.TaxonomyPosition;
import com.strandls.taxonomy.pojo.response.TaxonomyRegistryResponse;
import com.strandls.taxonomy.service.impl.TaxonomyESOperation;

public class TaxonomyBulkMappingThread implements Runnable {

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
	private TaxonomyEventProducer taxonomyEventProducer;

	public TaxonomyBulkMappingThread(Boolean selectAll, String bulkAction, String bulkTaxonIds, String bulkPosition,
			TaxonomyDefinitionDao taxonomyDefinitionDao, AcceptedSynonymDao acceptedSynonymDao,
			CommonNameDao commonNameDao, TaxonomyESOperation taxonomyESUpdate, TaxonomyRegistryDao taxonomyRegistryDao,
			EsServicesApi esServicesApi, TaxonomyEventProducer taxonomyEventProducer) {
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
		this.taxonomyEventProducer = taxonomyEventProducer;
	}

	@Override
	public void run() {

		List<Long> taxonIds = new ArrayList<Long>();

		if (bulkTaxonIds != null && !bulkTaxonIds.isEmpty() && Boolean.FALSE.equals(selectAll)) {
			taxonIds.addAll(Arrays.stream(bulkTaxonIds.split(",")).map(Long::valueOf).collect(Collectors.toList()));
		}

		if (!bulkAction.isEmpty() && (bulkAction.contains("position"))) {
			List<TaxonomyDefinition> taxonDataList = new ArrayList<TaxonomyDefinition>();
			if (bulkPosition != null && !bulkPosition.isEmpty()) {
				if (!taxonIds.isEmpty()) {
					taxonDataList = taxonomyDefinitionDao.fetchByListOfIds(taxonIds);

				}
				List<TaxonomyDefinition> TaxonList = new ArrayList<TaxonomyDefinition>();
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
				}
			}
		}

		if (!bulkAction.isEmpty() && (bulkAction.contains("merge"))) {
			List<TaxonomyRegistry> taxonDataList = new ArrayList<>();
			Long taxonId = taxonIds.remove(0);
			if (!taxonIds.isEmpty()) {
				taxonDataList = taxonomyRegistryDao.fetchByListOfTaxonomyIds(taxonIds);

			}
			TaxonomyRegistry mergeRegistry = taxonomyRegistryDao.findbyTaxonomyId(taxonId, null);
			List<TaxonomyRegistryResponse> hierar = taxonomyRegistryDao.getPathToRoot(taxonId, null);
			List<Breadcrumb> breadCrumbs = new ArrayList<>();
			for (TaxonomyRegistryResponse crumb : hierar) {
				Breadcrumb breadCrumb = new Breadcrumb();
				breadCrumb.setTaxonId(Long.parseLong(crumb.getId()));
				breadCrumb.setTaxonName(crumb.getName());
				breadCrumb.setTaxonRank(crumb.getRank());
				breadCrumbs.add(breadCrumb);
			}
			List<TaxonomyRegistry> TaxonList = new ArrayList<TaxonomyRegistry>();
			;
			Integer count = 0;

			if (Boolean.FALSE.equals(selectAll)) {
				while (count < taxonDataList.size()) {
					TaxonList.add(taxonDataList.get(count));

					if (TaxonList.size() >= 200) {
						bulkMergeAction(TaxonList, mergeRegistry.getPath(), taxonId, breadCrumbs);
						TaxonList.clear();
					}
					count++;
				}

				bulkMergeAction(TaxonList, mergeRegistry.getPath(), taxonId, breadCrumbs);
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
				taxon.setPosition(taxonomyPosition.name());
				taxon = taxonomyDefinitionDao.update(taxon);
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

	private void bulkMergeAction(List<TaxonomyRegistry> taxonList, String path, Long taxonId,
			List<Breadcrumb> breadCrumbs) {
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
		}
		taxonomyDefinitionDao.deleteByIds(deleteTaxonIds);
		taxonomyESUpdate.pushToElastic(taxonIds);
		List<String> deleteIds = deleteTaxonIds.stream().map(String::valueOf).collect(Collectors.toList());
		try {
			esServicesApi.bulkDelete("extended_taxon_definition", "_doc", deleteIds);
		} catch (ApiException e) {
			e.printStackTrace();
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		String timestamp = sdf.format(new Date());
		TaxonomyUpdateData taxonomyData = new TaxonomyUpdateData();
		taxonomyData.setTargetId(taxonId);
		taxonomyData.setBulkIds(deleteTaxonIds);
		taxonomyData.setNewId(taxonId);
		taxonomyData.setTimestamp(timestamp);
		taxonomyData.setBreadCrumbs(breadCrumbs);
		taxonomyEventProducer.sendTaxonomyUpdate(taxonomyData, true, true);
	}
}
