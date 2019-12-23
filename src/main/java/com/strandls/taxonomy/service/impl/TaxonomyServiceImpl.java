/**
 * 
 */
package com.strandls.taxonomy.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.strandls.taxonomy.dao.SpeciesGroupDao;
import com.strandls.taxonomy.dao.SpeciesGroupMappingDao;
import com.strandls.taxonomy.dao.SpeciesPermissionDao;
import com.strandls.taxonomy.dao.TaxonomyDefinitionDao;
import com.strandls.taxonomy.dao.TaxonomyRegistryDao;
import com.strandls.taxonomy.pojo.BreadCrumb;
import com.strandls.taxonomy.pojo.SpeciesGroup;
import com.strandls.taxonomy.pojo.SpeciesGroupMapping;
import com.strandls.taxonomy.pojo.SpeciesPermission;
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
	private SpeciesPermissionDao speciesPermissionDao;

	@Override
	public TaxonomyDefinition fetchById(Long id) {
		TaxonomyDefinition taxonomy = taxonomyDao.findById(id);
		return taxonomy;
	}

	@Override
	public List<BreadCrumb> fetchByTaxonomyId(Long id) {
		TaxonomyRegistry taxoRegistry = taxonomyRegistryDao.findbyTaxonomyId(id);

		String paths = taxoRegistry.getPath().replace("_", ",");
		List<BreadCrumb> breadCrumbs = new ArrayList<BreadCrumb>();
		List<TaxonomyDefinition> breadCrumbLists = taxonomyDao.breadCrumbSearch(paths);
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
		List<String> allTaxonomyList = new ArrayList<String>();
		for (String taxonId : taxonList) {
			TaxonomyRegistry taxoRegistry = taxonomyRegistryDao.findbyTaxonomyId(Long.parseLong(taxonId));
			String path[] = taxoRegistry.getPath().split("_");
			for (int i = 0; i < path.length; i++) {
				for (SpeciesGroupMapping speciesGroup : traitList) {
					if (speciesGroup.getTaxonConceptId() != null
							&& speciesGroup.getTaxonConceptId().toString().equals(path[i])) {
						i = 0;
						while (i < path.length) {
							allTaxonomyList.add(path[i]);
							i++;
						}
						break;
					}
				}
			}
		}
		return allTaxonomyList;
	}

	@Override
	public List<SpeciesGroup> findAllSpecies() {
		List<SpeciesGroup> result = speciesGroupDao.findAll();
		return result;
	}

	@Override
	public Boolean checkValidatePermission(Long userId, Long taxonomyId) {

		List<BreadCrumb> breadCrumbList = fetchByTaxonomyId(taxonomyId);
		List<SpeciesPermission> allowedSpeciesPermissionList = speciesPermissionDao.findByUserId(userId);

		List<Long> breadCrumbTaxonomyId = new ArrayList<Long>();
		List<Long> allowedSpeciesTaxonomyList = new ArrayList<Long>();
		for (BreadCrumb breadCrumb : breadCrumbList) {
			breadCrumbTaxonomyId.add(breadCrumb.getId());
		}
		for (SpeciesPermission speciesPermission : allowedSpeciesPermissionList) {
			allowedSpeciesTaxonomyList.add(speciesPermission.getTaxonConceptId());
		}
		for (Long permissionTaxonomy : allowedSpeciesTaxonomyList) {
			if (breadCrumbTaxonomyId.contains(permissionTaxonomy))
				return true;
		}
		return false;
	}

}
