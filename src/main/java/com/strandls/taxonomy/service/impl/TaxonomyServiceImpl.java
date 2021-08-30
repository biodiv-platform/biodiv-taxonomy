/**
 * 
 */
package com.strandls.taxonomy.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import com.strandls.taxonomy.dao.AcceptedSynonymDao;
import com.strandls.taxonomy.dao.SpeciesGroupDao;
import com.strandls.taxonomy.dao.SpeciesGroupMappingDao;
import com.strandls.taxonomy.dao.SpeciesPermissionDao;
import com.strandls.taxonomy.dao.TaxonomyDefinitionDao;
import com.strandls.taxonomy.dao.TaxonomyRegistryDao;
import com.strandls.taxonomy.pojo.AcceptedSynonym;
import com.strandls.taxonomy.pojo.BreadCrumb;
import com.strandls.taxonomy.pojo.SpeciesGroup;
import com.strandls.taxonomy.pojo.SpeciesGroupMapping;
import com.strandls.taxonomy.pojo.SpeciesPermission;
import com.strandls.taxonomy.pojo.TaxonTree;
import com.strandls.taxonomy.pojo.TaxonomyDefinition;
import com.strandls.taxonomy.pojo.TaxonomyRegistry;
import com.strandls.taxonomy.service.TaxonomySerivce;

/**
 * @author Abhishek Rudra
 *
 */
public class TaxonomyServiceImpl implements TaxonomySerivce {

	@Inject
	private TaxonomyDefinitionDao taxonomyDao;

	@Inject
	private TaxonomyRegistryDao taxonomyRegistryDao;

	@Inject
	private SpeciesGroupMappingDao speciesMappingDao;

	@Inject
	private SpeciesGroupDao speciesGroupDao;

	@Inject
	private AcceptedSynonymDao acceptedSynonymDao;

	@Inject
	private SpeciesPermissionDao speciesPermissionDao;

	@Override
	public TaxonomyDefinition fetchById(Long id) {
		return taxonomyDao.findById(id);
	}

	@Override
	public List<BreadCrumb> fetchByTaxonomyId(Long id) {
		TaxonomyRegistry taxoRegistry = taxonomyRegistryDao.findbyTaxonomyId(id);
		if (taxoRegistry == null) {
			AcceptedSynonym acceptedSynonym = acceptedSynonymDao.findAccpetedId(id);
			taxoRegistry = taxonomyRegistryDao.findbyTaxonomyId(acceptedSynonym.getAcceptedId());
		}

		List<BreadCrumb> breadCrumbs = new ArrayList<>();
		List<Long> pathList = new ArrayList<>();
		for (String path : taxoRegistry.getPath().split("_"))
			pathList.add(Long.parseLong(path));
		List<TaxonomyDefinition> breadCrumbLists = taxonomyDao.breadCrumbSearch(pathList);
		for (TaxonomyDefinition td : breadCrumbLists) {
			BreadCrumb breadCrumb = new BreadCrumb(td.getId(), td.getNormalizedForm());
			breadCrumbs.add(breadCrumb);
		}

		return breadCrumbs;
	}

	@Override
	public List<String> fetchBySpeciesId(Long id, List<String> taxonList) {
		List<SpeciesGroupMapping> traitList = speciesMappingDao.getTaxonomyId(id);
		for (SpeciesGroupMapping speciesGroup : traitList) {
			if (speciesGroup.getTaxonConceptId() != null)
				taxonList.add(speciesGroup.getTaxonConceptId().toString());
		}
		List<String> allTaxonomyList = new ArrayList<>();
		for (String taxonId : taxonList) {
			TaxonomyRegistry taxoRegistry = taxonomyRegistryDao.findbyTaxonomyId(Long.parseLong(taxonId));
			String[] path = taxoRegistry.getPath().split("_");
			for (int i = 0; i < path.length; i++) {
				for (SpeciesGroupMapping speciesGroup : traitList) {
					if (speciesGroup.getTaxonConceptId() != null
							&& speciesGroup.getTaxonConceptId().toString().equals(path[i])) {
						allTaxonomyList.addAll(Arrays.asList(path));
						break;
					}
				}
			}
		}
		return allTaxonomyList;
	}

	@Override
	public List<SpeciesGroup> findAllSpecies() {
		return speciesGroupDao.findAllOrdered();
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
	public List<SpeciesPermission> getSpeciesPermissions(Long userId) {
		return speciesPermissionDao.findByUserId(userId);
	}

}
