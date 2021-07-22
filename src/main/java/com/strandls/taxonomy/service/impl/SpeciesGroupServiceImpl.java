package com.strandls.taxonomy.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.strandls.taxonomy.TreeRoles;
import com.strandls.taxonomy.dao.SpeciesGroupDao;
import com.strandls.taxonomy.dao.SpeciesGroupMappingDao;
import com.strandls.taxonomy.dao.SpeciesPermissionDao;
import com.strandls.taxonomy.dao.TaxonomyRegistryDao;
import com.strandls.taxonomy.pojo.SpeciesGroup;
import com.strandls.taxonomy.pojo.SpeciesGroupMapping;
import com.strandls.taxonomy.pojo.SpeciesPermission;
import com.strandls.taxonomy.service.SpeciesGroupService;
import com.strandls.taxonomy.util.AbstractService;

public class SpeciesGroupServiceImpl extends AbstractService<SpeciesGroup> implements SpeciesGroupService {

	@Inject
	private SpeciesGroupMappingDao speciesMappingDao;

	@Inject
	private SpeciesGroupDao speciesGroupDao;

	@Inject
	private SpeciesPermissionDao speciesPermissionDao;

	@Inject
	private TaxonomyRegistryDao taxonomyRegistryDao;

	@Inject
	public SpeciesGroupServiceImpl(SpeciesGroupDao dao) {
		super(dao);
	}

	@Override
	public SpeciesGroup getGroupByTaxonId(Long taxonId) {
		Long speciesGroupId = speciesGroupDao.getGroupIdByTaxonId(taxonId);
		return findById(speciesGroupId);
	}
	
	@Override
	public SpeciesGroupMapping save(SpeciesGroupMapping speciesGroupMapping) {
		return speciesMappingDao.save(speciesGroupMapping);
	}

	@Override
	public SpeciesPermission save(SpeciesPermission speciesPermission) {
		return speciesPermissionDao.save(speciesPermission);
	}

	@Override
	public List<String> fetchBySpeciesGroupId(Long id, List<String> traitTaxonList) {
		Set<String> speciesGroupTaxons = speciesMappingDao.getTaxonIds(id);
		traitTaxonList.addAll(speciesGroupTaxons);
		if (traitTaxonList.isEmpty())
			return new ArrayList<>();
		List<Long> traitTaxonIds = new ArrayList<>();
		for (String taxonId : traitTaxonList)
			traitTaxonIds.add(Long.parseLong(taxonId));
		List<String> result = taxonomyRegistryDao.findByTaxonIdOnTraitList(traitTaxonIds, speciesGroupTaxons);
		traitTaxonList.retainAll(result);
		return traitTaxonList;
	}

	@Override
	public List<SpeciesGroup> findAllSpecies() {
		return speciesGroupDao.findAllOrdered();
	}

	@Override
	public List<SpeciesPermission> getSpeciesPermissions(Long userId) {
		return speciesPermissionDao.findByUserId(userId);
	}

	@Override
	public Boolean checkPermission(Long userId, Long taxonId, TreeRoles roles) {
		return speciesPermissionDao.checkPermission(userId, taxonId, roles);
	}

}
