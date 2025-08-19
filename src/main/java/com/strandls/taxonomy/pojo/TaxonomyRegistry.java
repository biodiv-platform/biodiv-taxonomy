package com.strandls.taxonomy.pojo;

import java.io.Serializable;
import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "taxonomy_registry")
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Registry entry mapping a taxonomy node to a classification and path using PostgreSQL ltree")
public class TaxonomyRegistry implements Serializable, Cloneable {

	private static final long serialVersionUID = -1891934272853024930L;

	@Schema(description = "Registry ID")
	private Long id;

	@Schema(description = "Classification (tree) ID this registry belongs to")
	private Long classificationId;

	@Schema(description = "PostgreSQL ltree path string for fast ancestor/descendant queries")
	private String path;

	@Schema(description = "ID of the referenced taxonomy definition")
	private Long taxonomyDefinationId;

	@Schema(description = "Rank (e.g. species, genus)", required = true)
	private String rank;

	@Schema(description = "Timestamp of record upload/creation")
	private Timestamp uploadTime;

	@Schema(description = "User ID who uploaded/created the record")
	private Long uploaderId;

	public TaxonomyRegistry() {
	}

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "taxonomy_registry_id_generator")
	@SequenceGenerator(name = "taxonomy_registry_id_generator", sequenceName = "taxonomy_registry_id_seq", allocationSize = 1)
	@Column(name = "id")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "classification_id")
	public Long getClassificationId() {
		return classificationId;
	}

	public void setClassificationId(Long classificationId) {
		this.classificationId = classificationId;
	}

	@Column(name = "path", columnDefinition = "ltree")
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Column(name = "taxon_definition_id")
	public Long getTaxonomyDefinationId() {
		return taxonomyDefinationId;
	}

	public void setTaxonomyDefinationId(Long taxonomyDefinationId) {
		this.taxonomyDefinationId = taxonomyDefinationId;
	}

	@Column(name = "rank", nullable = false)
	public String getRank() {
		return rank;
	}

	public void setRank(String rank) {
		this.rank = rank;
	}

	@Column(name = "upload_time")
	public Timestamp getUploadTime() {
		return uploadTime;
	}

	public void setUploadTime(Timestamp uploadTime) {
		this.uploadTime = uploadTime;
	}

	@Column(name = "uploader_id")
	public Long getUploaderId() {
		return uploaderId;
	}

	public void setUploaderId(Long uploaderId) {
		this.uploaderId = uploaderId;
	}

	@Override
	public TaxonomyRegistry clone() throws CloneNotSupportedException {
		return (TaxonomyRegistry) super.clone();
	}
}
