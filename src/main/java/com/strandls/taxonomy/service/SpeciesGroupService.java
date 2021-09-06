package com.strandls.taxonomy.service;

import java.util.List;

import com.strandls.taxonomy.TreeRoles;
import com.strandls.taxonomy.pojo.SpeciesGroup;
import com.strandls.taxonomy.pojo.SpeciesGroupMapping;
import com.strandls.taxonomy.pojo.SpeciesPermission;

/**
 * 
 * @author vilay
 *
 */
public interface SpeciesGroupService {

	public List<String> fetchBySpeciesGroupId(Long id, List<String> taxonList);

	public List<SpeciesGroup> findAllSpecies();

	public List<SpeciesPermission> getSpeciesPermissions(Long userId);

	public SpeciesGroup save(SpeciesGroup speciesGroup);

	public SpeciesGroupMapping save(SpeciesGroupMapping speciesGroupMapping);

	public SpeciesPermission save(SpeciesPermission speciesPermission);

	public Boolean checkPermission(Long userId, Long taxonId, TreeRoles roles);

	public SpeciesGroup getGroupByTaxonId(Long taxonId);

}
