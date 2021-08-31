package com.strandls.taxonomy.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strandls.esmodule.ApiException;
import com.strandls.esmodule.controllers.EsServicesApi;
import com.strandls.esmodule.pojo.MapQueryResponse;
import com.strandls.taxonomy.TaxonomyConfig;
import com.strandls.taxonomy.dao.TaxonomyDefinitionDao;
import com.strandls.taxonomy.pojo.TaxonomyESDocument;

public class TaxonomyESOperation {

	private final Logger logger = LoggerFactory.getLogger(TaxonomyESOperation.class);
	
	@Inject
	private SessionFactory sessionFactory;

	@Inject
	private ObjectMapper objectMapper;

	@Inject
	private EsServicesApi esServicesApi;
	
	@Inject
	private TaxonomyDefinitionDao taxonomyDefinitionDao;

	private static final String ES_TAXONOMY_INDEX = "es.taxonomy.index";
	private static final String ES_TAXONOMY_TYPE = "es.taxonomy.type";

	private static final String SQL_FILE = "extendedTaxonDefinition.sql";
	
	private static final int FIXED_THREAD_SIZE = 10;
	private static final int BATCH_SIZE = 1000;

	private List<MapQueryResponse> esResult = new ArrayList<>();
	
	private static String qryString;

	/**
	 * 
	 * @param taxonIds       - List of taxon Ids
	 * @param createOrUpdate - true - If the taxonIds need to be created in the
	 *                       elastic. false - If the taxonIds need to be updated in
	 *                       the elastic.
	 * @return - MapQueryResponse
	 */
	public List<MapQueryResponse> pushToElastic(List<Long> taxonIds) {
		ExecutorService executor = Executors.newFixedThreadPool(FIXED_THREAD_SIZE);
		try {
			qryString = TaxonomyConfig.fetchFileAsString(SQL_FILE);
		} catch (IOException e) {
			e.printStackTrace();
		}

		int size = taxonIds.size();
		
		int numBatches = size/BATCH_SIZE;
		
		for(int i=0;i<numBatches;i++) {
			List<Long> batch = taxonIds.subList(i*BATCH_SIZE, (i+1)*BATCH_SIZE);
			ESThread esThread = new ESThread(this, batch);
			executor.execute(esThread);
		}
		
		// Execute for the last batch of taxonIds
		List<Long> batch = taxonIds.subList(numBatches*BATCH_SIZE, size);
		if(!batch.isEmpty()) {
			ESThread esThread = new ESThread(this, batch);
			executor.execute(esThread);
		}
		
		executor.shutdown();
		
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			logger.error("Could not execute all the thread");
			Thread.currentThread().interrupt();
		}
		return esResult;
	}
	
	/** 
	 * This method is used by thread
	 * @param mapQueryResponses
	 */
	protected synchronized void updateResult(List<MapQueryResponse> mapQueryResponses) {
		esResult.addAll(mapQueryResponses);
	}
	
	/**
	 * This method is used by thread to execute the elastic update for taxonomy
	 * @param taxonIds
	 * @return
	 */
	protected List<MapQueryResponse> pushInBatches(List<Long> taxonIds) {
		
		Session session = sessionFactory.openSession();
		session.beginTransaction();
		
		try {
			Query<TaxonomyESDocument> query = session.createNativeQuery(qryString, TaxonomyESDocument.class);
			query.setParameterList("taxonIds", taxonIds);
			List<TaxonomyESDocument> taxonomyESDocuments = query.getResultList();
			
			String index = TaxonomyConfig.getString(ES_TAXONOMY_INDEX);
			String type = TaxonomyConfig.getString(ES_TAXONOMY_TYPE);
			
			String taxonomyJsonData = objectMapper.writeValueAsString(taxonomyESDocuments);
			return esServicesApi.bulkUpload(index, type, taxonomyJsonData);
			
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ApiException e) {
			e.printStackTrace();
		} finally {
			session.close();
		}
		
		return new ArrayList<>();
	}

	public List<MapQueryResponse> reIndexElastic() {
		Long rowCount = taxonomyDefinitionDao.getRowCount();
		int limit = BATCH_SIZE * 50;
		
		int batches = rowCount.intValue()/limit;
		for(int i = 0 ;i <= batches; i++) {
			int offset = limit * i;
			if(rowCount.intValue() < (offset + limit)) {
				limit = rowCount.intValue() - offset;
			}
			List<Long> taxonIds = taxonomyDefinitionDao.getAllIds(limit, offset);
			if(!taxonIds.isEmpty()) {
				pushToElastic(taxonIds);
			}
		}
		return esResult;
	}

}


class ESThread extends TaxonomyESOperation implements Runnable{
	
	private List<Long> taxonIds;
	private TaxonomyESOperation taxonomyESOperation;
	
	public ESThread(TaxonomyESOperation taxonomyESOperation, List<Long> taxonIds) {
		this.taxonomyESOperation = taxonomyESOperation;
		this.taxonIds = taxonIds;
	}
	
	@Override
	public void run() {
		List<MapQueryResponse> mapQueryResponses = taxonomyESOperation.pushInBatches(taxonIds);
		updateResult(mapQueryResponses);
	}
}