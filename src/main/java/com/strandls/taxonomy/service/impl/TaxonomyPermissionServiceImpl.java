/**
 * 
 */
package com.strandls.taxonomy.service.impl;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strandls.authentication_utility.util.AuthUtil;
import com.strandls.taxonomy.TreeRoles;
import com.strandls.taxonomy.dao.SpeciesPermissionDao;
import com.strandls.taxonomy.dao.SpeciesPermissionRequestDao;
import com.strandls.taxonomy.dao.TaxonomyDefinitionDao;
import com.strandls.taxonomy.pojo.EncryptedKey;
import com.strandls.taxonomy.pojo.PermissionData;
import com.strandls.taxonomy.pojo.SpeciesPermission;
import com.strandls.taxonomy.pojo.SpeciesPermissionRequest;
import com.strandls.taxonomy.pojo.TaxonomyDefinition;
import com.strandls.taxonomy.pojo.response.BreadCrumb;
import com.strandls.taxonomy.service.TaxonomyPermisisonService;
import com.strandls.taxonomy.service.TaxonomyRegistryService;
import com.strandls.taxonomy.util.EncryptionUtils;
import com.strandls.taxonomy.util.MailUtils;
import com.strandls.user.controller.UserServiceApi;
import com.strandls.user.pojo.User;

import net.minidev.json.JSONArray;

/**
 * @author Abhishek Rudra
 *
 * 
 */
public class TaxonomyPermissionServiceImpl implements TaxonomyPermisisonService {

	private final Logger logger = LoggerFactory.getLogger(TaxonomyPermissionServiceImpl.class);

	@Inject
	private ObjectMapper om;

	@Inject
	private EncryptionUtils encryptUtils;

	@Inject
	private TaxonomyRegistryService registryService;

	@Inject
	private SpeciesPermissionDao speciesPermissionDao;

	@Inject
	private SpeciesPermissionRequestDao permissionReqDao;

	@Inject
	private MailUtils mailUtils;

	@Inject
	private TaxonomyDefinitionDao taxDefinationDao;

	@Inject
	private UserServiceApi userService;

	@Override
	public Boolean getPermissionOnTree(HttpServletRequest request, Long taxonId) {
		CommonProfile profile = AuthUtil.getProfileFromRequest(request);
		Long userId = Long.parseLong(profile.getId());
		List<BreadCrumb> breadcrumbs = registryService.fetchByTaxonomyId(taxonId);
		Boolean permission = false;
		for (BreadCrumb crumb : breadcrumbs) {
			permission = speciesPermissionDao.checkPermission(userId, crumb.getId(), TreeRoles.SPECIESCONTRIBUTOR);
			if (permission.booleanValue())
				break;
		}

		return permission;
	}

	@Override
	public Boolean assignUpdatePermissionDirectly(HttpServletRequest request, PermissionData permissionData) {
		CommonProfile profile = AuthUtil.getProfileFromRequest(request);
		JSONArray userRole = (JSONArray) profile.getAttribute("roles");
		if (userRole.contains("ROLE_ADMIN")) {

			TreeRoles role = TreeRoles.valueOf(permissionData.getRole());

			if (role == null)
				return false;

			SpeciesPermission hasPermission = speciesPermissionDao.findPermissionOntaxon(permissionData.getUserId(),
					permissionData.getTaxonId());

//			deleting the req if already it was raised
			SpeciesPermissionRequest isExist = permissionReqDao.requestPermissionExist(permissionData.getUserId(),
					permissionData.getTaxonId(), role);
			if (isExist != null) {
				permissionReqDao.delete(isExist);
			}

			if (hasPermission == null) {
//			no previous permission, create a new permission
				SpeciesPermission speciesPermission = new SpeciesPermission(null, 0L, permissionData.getUserId(),
						new Date(), role.getValue(), permissionData.getTaxonId());
				speciesPermissionDao.save(speciesPermission);
			} else {
				hasPermission.setPermissionType(role.getValue());
				speciesPermissionDao.update(hasPermission);

			}
			return true;

		}
		return false;
	}

	@Override
	public Boolean requestPermission(HttpServletRequest request, PermissionData permissionData) {
		CommonProfile profile = AuthUtil.getProfileFromRequest(request);
		Long userId = Long.parseLong(profile.getId());
		TreeRoles role = TreeRoles.valueOf(permissionData.getRole());
		if (role == null)
			return false;

		Boolean alreadyHasPermission = speciesPermissionDao.checkPermission(userId, permissionData.getTaxonId(), role);
		if (alreadyHasPermission.booleanValue())
			return false;

		SpeciesPermissionRequest isExist = permissionReqDao.requestPermissionExist(userId, permissionData.getTaxonId(),
				role);
		if (isExist == null) {
			SpeciesPermissionRequest permissionRequest = new SpeciesPermissionRequest(null, permissionData.getTaxonId(),
					userId, role.getValue());
			permissionRequest = permissionReqDao.save(permissionRequest);
			sendMail(permissionRequest);
			return true;
		} else {
			if (!role.getValue().equalsIgnoreCase(isExist.getRole())) {
				isExist.setRole(role.getValue());
				isExist = permissionReqDao.update(isExist);
				sendMail(isExist);
				return true;
			}

		}

		return false;
	}

	private void sendMail(SpeciesPermissionRequest permissionReq) {

		String reqText;
		try {
			reqText = om.writeValueAsString(permissionReq);
			String encryptedKey = encryptUtils.encrypt(reqText);

			User requestee = userService.getUser(permissionReq.getUserId().toString());
			TaxonomyDefinition taxDef = taxDefinationDao.findById(permissionReq.getTaxonConceptId());
			List<User> requestors = userService.getAllAdmins();

			mailUtils.sendPermissionRequest(requestors, taxDef.getName(), taxDef.getId(), permissionReq.getRole(),
					requestee, encryptedKey);

		} catch (Exception e) {
			logger.error(e.getMessage());
		}

	}

	@Override
	public Boolean verifyPermissionGrant(HttpServletRequest request, EncryptedKey encryptedKey) {

		try {
			CommonProfile profile = AuthUtil.getProfileFromRequest(request);
			JSONArray userRoles = (JSONArray) profile.getAttribute("roles");
			if (userRoles.contains("ROLE_ADMIN")) {
				String reqdata = encryptUtils.decrypt(encryptedKey.getToken());
				SpeciesPermissionRequest permissionReq = om.readValue(reqdata, SpeciesPermissionRequest.class);
				SpeciesPermissionRequest permissionReqOriginal = permissionReqDao.findById(permissionReq.getId());
				if (permissionReqOriginal.equals(permissionReq)) {

					SpeciesPermission alreadyExist = speciesPermissionDao.findPermissionOntaxon(
							permissionReqOriginal.getUserId(), permissionReqOriginal.getTaxonConceptId());
					if (alreadyExist == null) {
						SpeciesPermission permission = new SpeciesPermission(null, 0L,
								permissionReqOriginal.getUserId(), new Date(), permissionReqOriginal.getRole(),
								permissionReqOriginal.getTaxonConceptId());
						speciesPermissionDao.save(permission);

					} else {
						if (!alreadyExist.getPermissionType().equalsIgnoreCase(permissionReqOriginal.getRole())) {
							alreadyExist.setPermissionType(permissionReqOriginal.getRole());
							speciesPermissionDao.update(alreadyExist);
						}
					}
					permissionReqDao.delete(permissionReqOriginal);

					User requestee = userService.getUser(permissionReq.getUserId().toString());
					TaxonomyDefinition taxDef = taxDefinationDao.findById(permissionReq.getTaxonConceptId());

					mailUtils.sendPermissionGrant(requestee, taxDef.getName(), permissionReq.getRole(),
							permissionReq.getTaxonConceptId());

					return true;
				}

			}

		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		return false;
	}
}
