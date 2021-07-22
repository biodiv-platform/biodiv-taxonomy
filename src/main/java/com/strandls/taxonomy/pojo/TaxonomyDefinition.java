/**
 * 
 */
package com.strandls.taxonomy.pojo;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.strandls.taxonomy.pojo.response.TaxonomyNamelistItem;

/**
 * @author Abhishek Rudra
 *
 */

@SqlResultSetMapping(name = "TaxonomyNameList", classes = {
		@ConstructorResult(targetClass = TaxonomyNamelistItem.class, columns = {
				@ColumnResult(name = "id", type = Long.class), @ColumnResult(name = "rank", type = String.class),
				@ColumnResult(name = "name", type = String.class), @ColumnResult(name = "status", type = String.class),
				@ColumnResult(name = "position", type = String.class), @ColumnResult(name = "rankvalue", type = Double.class)
				}) 
		})

@Entity
@Table(name = "taxonomy_definition", indexes = {
		@Index(name = "idx_canonical_form", columnList = "canonical_form, rank, is_deleted") })
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaxonomyDefinition {

	private Long id;
	private String binomialForm;
	private String canonicalForm;
	private String italicisedForm;
	private Long externalLinksId;
	private String name;
	private String normalizedForm;
	private String rank;
	private Timestamp uploadTime;
	private Long uploaderId;
	private String status;
	private String position;
	private String authorYear;
	private String matchDatabaseName;
	private String matchId;
	private String ibpSource;
	private String viaDatasource;
	private Boolean isFlagged;
	private String relationship;
	private String classs;
	private String flaggingReason;
	private Boolean isDeleted;
	private String dirtyListReason;
	private String activityDescription;
	private String defaultHierarchy;
	private String nameSourceId;

	public TaxonomyDefinition() {
		super();
	}

	@Id
	// @GeneratedValue
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "taxonomy_definition_id_generator")
	@SequenceGenerator(name = "taxonomy_definition_id_generator", sequenceName = "taxonomy_definition_id_seq", allocationSize = 1)
	@Column(name = "id")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "binomial_form")
	public String getBinomialForm() {
		return binomialForm;
	}

	public void setBinomialForm(String binomialForm) {
		this.binomialForm = binomialForm;
	}

	@Column(name = "canonical_form", nullable = false)
	public String getCanonicalForm() {
		return canonicalForm;
	}

	public void setCanonicalForm(String canonicalForm) {
		this.canonicalForm = canonicalForm;
	}

	@Column(name = "italicised_form", nullable = false)
	public String getItalicisedForm() {
		return italicisedForm;
	}

	public void setItalicisedForm(String italicisedForm) {
		this.italicisedForm = italicisedForm;
	}

	@Column(name = "external_links_id", insertable = false, updatable = false)
	public Long getExternalLinksId() {
		return externalLinksId;
	}

	public void setExternalLinksId(Long externalLinksId) {
		this.externalLinksId = externalLinksId;
	}

	@Column(name = "name", nullable = false)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "normalized_form")
	public String getNormalizedForm() {
		return normalizedForm;
	}

	public void setNormalizedForm(String normalizedForm) {
		this.normalizedForm = normalizedForm;
	}

	@Column(name = "rank")
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

	@Column(name = "status", nullable = false)
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(name = "position", nullable = false)
	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	@Column(name = "author_year")
	public String getAuthorYear() {
		return authorYear;
	}

	public void setAuthorYear(String authorYear) {
		this.authorYear = authorYear;
	}

	@Column(name = "match_database_name")
	@Type(type = "text")
	public String getMatchDatabaseName() {
		return matchDatabaseName;
	}

	public void setMatchDatabaseName(String matchDatabaseName) {
		this.matchDatabaseName = matchDatabaseName;
	}

	@Column(name = "match_id")
	public String getMatchId() {
		return matchId;
	}

	public void setMatchId(String matchId) {
		this.matchId = matchId;
	}

	@Column(name = "ibp_source")
	public String getIbpSource() {
		return ibpSource;
	}

	public void setIbpSource(String ibpSource) {
		this.ibpSource = ibpSource;
	}

	@Column(name = "via_datasource")
	@Type(type = "text")
	public String getViaDatasource() {
		return viaDatasource;
	}

	public void setViaDatasource(String viaDatasource) {
		this.viaDatasource = viaDatasource;
	}

	@Column(name = "is_flagged")
	public Boolean getIsFlagged() {
		return isFlagged;
	}

	public void setIsFlagged(Boolean isFlagged) {
		this.isFlagged = isFlagged;
	}

	@Column(name = "relationship")
	public String getRelationship() {
		return relationship;
	}

	public void setRelationship(String relationship) {
		this.relationship = relationship;
	}

	@Column(name = "class", nullable = false)
	public String getClasss() {
		return classs;
	}

	public void setClasss(String classs) {
		this.classs = classs;
	}

	@Column(name = "flagging_reason", length = 1500)
	public String getFlaggingReason() {
		return flaggingReason;
	}

	public void setFlaggingReason(String flaggingReason) {
		this.flaggingReason = flaggingReason;
	}

	@Column(name = "is_deleted")
	public Boolean getIsDeleted() {
		return isDeleted;
	}

	public void setIsDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	@Column(name = "dirty_list_reason", length = 1000)
	public String getDirtyListReason() {
		return dirtyListReason;
	}

	public void setDirtyListReason(String dirtyListReason) {
		this.dirtyListReason = dirtyListReason;
	}

	@Column(name = "activity_description", length = 2000)
	public String getActivityDescription() {
		return activityDescription;
	}

	public void setActivityDescription(String activityDescription) {
		this.activityDescription = activityDescription;
	}

	@Column(name = "default_hierarchy")
	@Type(type = "text")
	public String getDefaultHierarchy() {
		return defaultHierarchy;
	}

	public void setDefaultHierarchy(String defaultHierarchy) {
		this.defaultHierarchy = defaultHierarchy;
	}

	@Column(name = "name_source_id")
	public String getNameSourceId() {
		return nameSourceId;
	}

	public void setNameSourceId(String nameSourceId) {
		this.nameSourceId = nameSourceId;
	}
}
