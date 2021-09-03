/**
 * 
 */
package com.strandls.taxonomy.pojo;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Abhishek Rudra
 *
 */

@Entity
@Table(name = "species_permission")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpeciesPermission {

	private Long id;
	private Long version;
	private Long authorId;
	private Date createdOn;
	private Long permissionType;
	private Long taxonConceptId;

	/**
	 * 
	 */
	public SpeciesPermission() {
		super();
	}

	/**
	 * @param id
	 * @param version
	 * @param authorId
	 * @param createdOn
	 * @param permissionType
	 * @param taxonConceptId
	 */
	public SpeciesPermission(Long id, Long version, Long authorId, Date createdOn, Long permissionType,
			Long taxonConceptId) {
		super();
		this.id = id;
		this.version = version;
		this.authorId = authorId;
		this.createdOn = createdOn;
		this.permissionType = permissionType;
		this.taxonConceptId = taxonConceptId;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "version")
	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	@Column(name = "author_id")
	public Long getAuthorId() {
		return authorId;
	}

	public void setAuthorId(Long authorId) {
		this.authorId = authorId;
	}

	@Column(name = "created_on")
	public Date getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	@Column(name = "permission_type")
	public Long getPermissionType() {
		return permissionType;
	}

	public void setPermissionType(Long permissionType) {
		this.permissionType = permissionType;
	}

	@Column(name = "taxon_concept_id")
	public Long getTaxonConceptId() {
		return taxonConceptId;
	}

	public void setTaxonConceptId(Long taxonConceptId) {
		this.taxonConceptId = taxonConceptId;
	}

}
