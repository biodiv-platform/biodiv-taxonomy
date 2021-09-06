/**
 * 
 */
package com.strandls.taxonomy.pojo;

/**
 * @author Abhishek Rudra
 *
 * 
 */
public class PermissionData {

	private Long taxonId;
	private Long userId;
	private String role;

	/**
	 * 
	 */
	public PermissionData() {
		super();
	}

	/**
	 * @param taxonId
	 * @param userId
	 * @param role
	 */
	public PermissionData(Long taxonId, Long userId, String role) {
		super();
		this.taxonId = taxonId;
		this.userId = userId;
		this.role = role;
	}

	public Long getTaxonId() {
		return taxonId;
	}

	public void setTaxonId(Long taxonId) {
		this.taxonId = taxonId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

}
