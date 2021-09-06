/**
 * 
 */
package com.strandls.taxonomy.service;

import javax.servlet.http.HttpServletRequest;

import com.strandls.taxonomy.pojo.EncryptedKey;
import com.strandls.taxonomy.pojo.PermissionData;

/**
 * @author Abhishek Rudra
 *
 * 
 */
public interface TaxonomyPermisisonService {

	public Boolean getPermissionOnTree(HttpServletRequest request, Long taxonId);

	public Boolean checkIsContributor(HttpServletRequest request, Long taxonomyId);

	public Boolean assignUpdatePermissionDirectly(HttpServletRequest request, PermissionData permissionData);

	public Boolean requestPermission(HttpServletRequest request, PermissionData permissionData);

	public Boolean verifyPermissionGrant(HttpServletRequest request, EncryptedKey encryptedKey);
}
