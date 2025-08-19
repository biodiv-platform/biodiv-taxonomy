/** */
package com.strandls.taxonomy.pojo;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Abhishek Rudra
 */
@Entity
@Table(name = "species_permission_request")
@JsonIgnoreProperties(ignoreUnknown = false)
public class SpeciesPermissionRequest {

	private Long id;
	private Long taxonConceptId;
	private Long userId;
	private String role;

	/** */
	public SpeciesPermissionRequest() {
		super();
	}

	/**
	 * @param id
	 * @param taxonConceptId
	 * @param userId
	 * @param role
	 */
	public SpeciesPermissionRequest(Long id, Long taxonConceptId, Long userId, String role) {
		super();
		this.id = id;
		this.taxonConceptId = taxonConceptId;
		this.userId = userId;
		this.role = role;
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

	@Column(name = "taxon_id")
	public Long getTaxonConceptId() {
		return taxonConceptId;
	}

	public void setTaxonConceptId(Long taxonConceptId) {
		this.taxonConceptId = taxonConceptId;
	}

	@Column(name = "user_id")
	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	@Column(name = "role")
	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, role, taxonConceptId, userId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SpeciesPermissionRequest other = (SpeciesPermissionRequest) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (role == null) {
			if (other.role != null)
				return false;
		} else if (!role.equals(other.role))
			return false;
		if (taxonConceptId == null) {
			if (other.taxonConceptId != null)
				return false;
		} else if (!taxonConceptId.equals(other.taxonConceptId))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}
}
