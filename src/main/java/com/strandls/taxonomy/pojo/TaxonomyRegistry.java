/**
 * 
 */
package com.strandls.taxonomy.pojo;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Abhishek Rudra
 *
 */

@Entity
@Table(name = "taxonomy_registry")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaxonomyRegistry implements Serializable, Cloneable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1891934272853024930L;
	
	private Long id;
	private Long classificationId;
	private String path;
	private Long taxonomyDefinationId;
	private String rank;
	private Timestamp uploadTime;
	private Long uploaderId;

	@Id
	//@GeneratedValue
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
	@Type(type = "com.strandls.taxonomy.pojo.enumtype.LTreeType")
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
		Object object = super.clone();
		if(object instanceof TaxonomyRegistry) {
			return (TaxonomyRegistry) super.clone();
		}
		return this;
	}
}
