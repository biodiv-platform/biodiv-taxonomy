package com.strandls.taxonomy.pojo;

public class UserTaxonRoleMapping {

	private Long userid;
	private Long roleid;
	private String role;
	private Long[] taxonomyids;

	public UserTaxonRoleMapping() {
		super();
	}

	/**
	 * 
	 * @param userid
	 * @param roleid
	 * @param role
	 * @param taxonomyids
	 */
	public UserTaxonRoleMapping(Long userid, Long roleid, String role, Long[] taxonomyids) {
		super();
		this.userid = userid;
		this.roleid = roleid;
		this.role = role;
		this.taxonomyids = taxonomyids;
	}

	public Long getUserid() {
		return userid;
	}

	public void setUserid(Long userid) {
		this.userid = userid;
	}

	public Long getRoleid() {
		return roleid;
	}

	public void setRoleid(Long roleid) {
		this.roleid = roleid;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public Long[] getTaxonomyids() {
		return taxonomyids;
	}

	public void setTaxonomyids(Long[] taxonomyids) {
		this.taxonomyids = taxonomyids;
	}

}