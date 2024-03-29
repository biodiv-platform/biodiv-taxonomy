/**
 * 
 */
package com.strandls.taxonomy.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;

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
import com.strandls.taxonomy.util.EsUserSpeciesPermissionUpdate;
import com.strandls.taxonomy.util.MailUtils;
import com.strandls.taxonomy.util.TaxonomyUtil;
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

	@Inject
	private EsUserSpeciesPermissionUpdate userPermissionUpdate;

	private Map<TreeRoles, Long> roleIdMap = TaxonomyUtil.getRoleIdMap();

	@Override
	public Boolean getPermissionOnTree(HttpServletRequest request, Long taxonId) {
		CommonProfile profile = AuthUtil.getProfileFromRequest(request);
		Long userId = Long.parseLong(profile.getId());
		List<BreadCrumb> breadcrumbs = registryService.fetchByTaxonomyId(taxonId);
		Boolean permission = false;
		for (BreadCrumb crumb : breadcrumbs) {
//			for species contributor role
			permission = speciesPermissionDao.checkPermission(userId, crumb.getId(), TreeRoles.SPECIESCONTRIBUTOR);
			if (permission.booleanValue())
				break;

//			for taxonomy contrbutor role
			permission = speciesPermissionDao.checkPermission(userId, crumb.getId(), TreeRoles.TAXONOMYCONTRIBUTOR);
			if (permission.booleanValue())
				break;
		}

		return permission;
	}

	@Override
	public Boolean assignUpdatePermissionDirectly(HttpServletRequest request, PermissionData permissionData) {

		try {
			CommonProfile profile = AuthUtil.getProfileFromRequest(request);
			JSONArray userRole = (JSONArray) profile.getAttribute("roles");
			if (userRole.contains("ROLE_ADMIN")) {

				TreeRoles role = TreeRoles.valueOf(permissionData.getRole().replace(" ", ""));

				if (role == null)
					return false;

				SpeciesPermission hasPermission = speciesPermissionDao.findPermissionOntaxon(permissionData.getUserId(),
						permissionData.getTaxonId());

//				deleting the req if already it was raised
				SpeciesPermissionRequest isExist = permissionReqDao.requestPermissionExist(permissionData.getUserId(),
						permissionData.getTaxonId(), role);
				if (isExist != null) {
					permissionReqDao.delete(isExist);
				}

				if (hasPermission == null) {
//				no previous permission, create a new permission
					SpeciesPermission speciesPermission = new SpeciesPermission(null, 0L, permissionData.getUserId(),
							new Date(), roleIdMap.get(role), permissionData.getTaxonId());
					speciesPermissionDao.save(speciesPermission);
					userPermissionUpdate.speciesUserPermissionEsUpdate(permissionData.getUserId());
				} else {
					hasPermission.setPermissionType(roleIdMap.get(role));
					speciesPermissionDao.update(hasPermission);
					userPermissionUpdate.speciesUserPermissionEsUpdate(permissionData.getUserId());

				}

				User requestee = userService.getUser(permissionData.getUserId().toString());
				TaxonomyDefinition taxDef = taxDefinationDao.findById(permissionData.getTaxonId());

				mailUtils.sendPermissionGrant(requestee, taxDef.getName(), role.getValue(),
						permissionData.getTaxonId());
				return true;

			}
			return false;
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return false;

	}

	@Override
	public Boolean requestPermission(HttpServletRequest request, PermissionData permissionData) {
		CommonProfile profile = AuthUtil.getProfileFromRequest(request);
		Long userId = Long.parseLong(profile.getId());
		TreeRoles role = TreeRoles.valueOf(permissionData.getRole().replace(" ", ""));
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
			sendMail(permissionRequest, permissionData.getRequestorMessage());
		} else {
			if (!role.getValue().equalsIgnoreCase(isExist.getRole())) {
				isExist.setRole(role.getValue());
				isExist = permissionReqDao.update(isExist);
			}
			sendMail(isExist, permissionData.getRequestorMessage());

		}
		return true;
	}

	private void sendMail(SpeciesPermissionRequest permissionReq, String requestorMessage) {

		String reqText;
		try {
			reqText = om.writeValueAsString(permissionReq);
			String encryptedKey = encryptUtils.encrypt(reqText);

			User requestee = userService.getUser(permissionReq.getUserId().toString());
			TaxonomyDefinition taxDef = taxDefinationDao.findById(permissionReq.getTaxonConceptId());
			List<User> requestors = userService.getAllAdmins();
			TreeRoles role = TreeRoles.valueOf(permissionReq.getRole().replace(" ", ""));

			mailUtils.sendPermissionRequest(requestors, taxDef.getName(), taxDef.getId(), role.getValue(), requestee,
					encryptedKey, requestorMessage);

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
				TreeRoles role = TreeRoles.valueOf(permissionReqOriginal.getRole().replace(" ", ""));
				if (permissionReqOriginal.equals(permissionReq)) {

					SpeciesPermission alreadyExist = speciesPermissionDao.findPermissionOntaxon(
							permissionReqOriginal.getUserId(), permissionReqOriginal.getTaxonConceptId());
					if (alreadyExist == null) {
						SpeciesPermission permission = new SpeciesPermission(null, 0L,
								permissionReqOriginal.getUserId(), new Date(), roleIdMap.get(role),
								permissionReqOriginal.getTaxonConceptId());
						speciesPermissionDao.save(permission);
						userPermissionUpdate.speciesUserPermissionEsUpdate(permissionReqOriginal.getUserId());

					} else {

						if (!alreadyExist.getPermissionType().equals(roleIdMap.get(role))) {
							alreadyExist.setPermissionType(roleIdMap.get(role));
							speciesPermissionDao.update(alreadyExist);
							userPermissionUpdate.speciesUserPermissionEsUpdate(permissionReqOriginal.getUserId());

						}
					}
					permissionReqDao.delete(permissionReqOriginal);

					User requestee = userService.getUser(permissionReq.getUserId().toString());
					TaxonomyDefinition taxDef = taxDefinationDao.findById(permissionReq.getTaxonConceptId());

					mailUtils.sendPermissionGrant(requestee, taxDef.getName(), role.getValue(),
							permissionReq.getTaxonConceptId());

					return true;
				}

			}

		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		return false;
	}

	@Override
	public Boolean checkIsContributor(HttpServletRequest request, Long taxonomyId) {
		try {
			CommonProfile profile = AuthUtil.getProfileFromRequest(request);
			JSONArray userRole = (JSONArray) profile.getAttribute("roles");
			Boolean isContributor = false;
			if (userRole.contains("ROLE_ADMIN")) {
				isContributor = true;
			} else {

				isContributor = getPermissionOnTree(request, taxonomyId);
			}
			return isContributor;
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return false;

	}

	@Override
	public Boolean checkIsObservationCurator(HttpServletRequest request, Long taxonomyId) {
		CommonProfile profile = AuthUtil.getProfileFromRequest(request);
		JSONArray userRole = (JSONArray) profile.getAttribute("roles");
		Long userId = Long.parseLong(profile.getId());
		if (userRole.contains("ROLE_ADMIN")) {
			return true;
		}

		List<BreadCrumb> breadcrumbs = registryService.fetchByTaxonomyId(taxonomyId);
		Boolean permission = false;
		for (BreadCrumb crumb : breadcrumbs) {
//			for observation curator role
			permission = speciesPermissionDao.checkPermission(userId, crumb.getId(), TreeRoles.OBSERVATIONCURATOR);
			if (permission.booleanValue())
				return permission;

		}
		return permission;

	}
}
