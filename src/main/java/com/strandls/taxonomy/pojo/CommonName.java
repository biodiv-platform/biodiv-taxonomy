package com.strandls.taxonomy.pojo;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.strandls.utility.pojo.Language;

/**
 * 
 * @author vilay
 *
 */

@Entity
@Table(name = "common_names")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonName {

	private Long id;
	private Long languageId;
	private String name;
	private Long taxonConceptId;
	private Date uploadTime;
	private Long uploaderId;
	private String transliteration;
	private Boolean isPreffered;
	private String viaDatasource;
	private Boolean isDeleted;
	private Language language;

	public CommonName() {
		super();
	}

	/**
	 * @param id
	 * @param languageId
	 * @param name
	 * @param taxonConceptId
	 * @param uploadTime
	 * @param uploaderId
	 * @param transliteration
	 * @param isPreffered
	 * @param viaDatasource
	 * @param isDeleted
	 */
	public CommonName(Long id, Long languageId, String name, Long taxonConceptId, Date uploadTime, Long uploaderId,
			String transliteration, Boolean isPreffered, String viaDatasource, Boolean isDeleted) {
		super();
		this.id = id;
		this.languageId = languageId;
		this.name = name;
		this.taxonConceptId = taxonConceptId;
		this.uploadTime = uploadTime;
		this.uploaderId = uploaderId;
		this.transliteration = transliteration;
		this.isPreffered = isPreffered;
		this.viaDatasource = viaDatasource;
		this.isDeleted = isDeleted;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "common_name_id_generator")
	@SequenceGenerator(name = "common_name_id_generator", sequenceName = "common_name_id_seq", allocationSize = 1)
	@Column(name = "id")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "language_id")
	public Long getLanguageId() {
		return languageId;
	}

	public void setLanguageId(Long languageId) {
		this.languageId = languageId;
	}

	@Column(name = "name", nullable = false)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "taxon_concept_id", nullable = false)
	public Long getTaxonConceptId() {
		return taxonConceptId;
	}

	public void setTaxonConceptId(Long taxonConceptId) {
		this.taxonConceptId = taxonConceptId;
	}

	@Column(name = "upload_time")
	public Date getUploadTime() {
		return uploadTime;
	}

	public void setUploadTime(Date uploadTime) {
		this.uploadTime = uploadTime;
	}

	@Column(name = "uploader_id")
	public Long getUploaderId() {
		return uploaderId;
	}

	public void setUploaderId(Long uploaderId) {
		this.uploaderId = uploaderId;
	}

	@Column(name = "transliteration")
	public String getTransliteration() {
		return transliteration;
	}

	public void setTransliteration(String transliteration) {
		this.transliteration = transliteration;
	}

	@Column(name = "is_preffered")
	public Boolean getIsPreffered() {
		return isPreffered;
	}

	public void setIsPreffered(Boolean isPreffered) {
		this.isPreffered = isPreffered;
	}

	@Column(name = "via_datasource")
	public String getViaDatasource() {
		return viaDatasource;
	}

	public void setViaDatasource(String viaDatasource) {
		this.viaDatasource = viaDatasource;
	}

	@Column(name = "is_deleted")
	public Boolean getIsDeleted() {
		return isDeleted;
	}

	public void setIsDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	@Transient
	public Language getLanguage() {
		return language;
	}

	public void setLanguage(Language language) {
		this.language = language;
	}

}
