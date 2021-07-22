package com.strandls.taxonomy.service;

import java.util.List;
import java.util.Map;

import com.strandls.taxonomy.pojo.response.BreadCrumb;
import com.strandls.taxonomy.pojo.response.TaxonRelation;
import com.strandls.taxonomy.pojo.response.TaxonTree;
import com.strandls.taxonomy.service.exception.TaxonCreationException;
import com.strandls.taxonomy.service.exception.UnRecongnizedRankException;
import com.strandls.utility.ApiException;

public interface TaxonomyRegistryService {

	public List<BreadCrumb> fetchByTaxonomyId(Long id);

	public List<TaxonTree> fetchTaxonTrees(List<Long> taxonList);

	public List<TaxonRelation> list(Long parent, String taxonIds, boolean expandTaxon, Long classificationId);

	
	/**
	 * Code below from here on is only for the migration purpose
	 */
	public Map<String, Object> migrateCleanName() throws CloneNotSupportedException;

	public Map<String, Object> snapWorkingNames() throws CloneNotSupportedException, UnRecongnizedRankException, ApiException, TaxonCreationException;

	public Map<String, Object> snapRawNames() throws CloneNotSupportedException, UnRecongnizedRankException, ApiException, TaxonCreationException;
}
