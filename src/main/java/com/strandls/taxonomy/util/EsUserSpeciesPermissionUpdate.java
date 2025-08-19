package com.strandls.taxonomy.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.strandls.esmodule.controllers.EsServicesApi;
import com.strandls.taxonomy.dao.SpeciesPermissionDao;
import com.strandls.taxonomy.pojo.SpeciesPermission;
import com.strandls.taxonomy.pojo.UserTaxonRoleMapping;
import com.strandls.user.ApiException;
import com.strandls.user.controller.RoleServiceApi;
import com.strandls.user.pojo.Role;

public class EsUserSpeciesPermissionUpdate {

	private final Logger logger = LoggerFactory.getLogger(EsUserSpeciesPermissionUpdate.class);
	private static final String INDEX = "extended_user";
	private static final String TYPE = "_doc";

	@Inject
	private RoleServiceApi roleService;

	@Inject
	private SpeciesPermissionDao speciesPermissionDao;

	@Inject
	private EsServicesApi esService;

	public void speciesUserPermissionEsUpdate(Long userId) throws ApiException {
		List<UserTaxonRoleMapping> ugRoleMapping = new ArrayList<UserTaxonRoleMapping>();
		List<Role> roles = roleService.getAllRoles();
		roles.forEach(role -> {
			List<SpeciesPermission> list = speciesPermissionDao.getTaxonIdByUserAndRole(userId, role.getId());
			List<Long> taxonList = list.stream().map(x -> x.getTaxonConceptId()).collect(Collectors.toList());
			UserTaxonRoleMapping taxonRoleMapping = new UserTaxonRoleMapping(userId, role.getId(), role.getAuthority(),
					taxonList);
			if (!taxonList.isEmpty())
				ugRoleMapping.add(taxonRoleMapping);
		});
		Map<String, Object> doc = new HashMap<String, Object>();
		doc.put("taxonomy", ugRoleMapping);
		try {
			esService.update(INDEX, TYPE, userId.toString(), doc);
		} catch (com.strandls.esmodule.ApiException e) {
			logger.error("Unable to update Es User Details " + e.getMessage());
		}
	}
}
