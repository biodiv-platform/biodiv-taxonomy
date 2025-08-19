package com.strandls.taxonomy.pojo;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.strandls.taxonomy.pojo.response.TaxonomyNamelistItem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

@Schema(description = "Full Taxonomy Definition entity: canonical info, authorship, system mapping, and status")
@SqlResultSetMapping(name = "TaxonomyNameList", classes = {
		@ConstructorResult(targetClass = TaxonomyNamelistItem.class, columns = {
				@ColumnResult(name = "id", type = Long.class), @ColumnResult(name = "rank", type = String.class),
				@ColumnResult(name = "name", type = String.class), @ColumnResult(name = "status", type = String.class),
				@ColumnResult(name = "position", type = String.class),
				@ColumnResult(name = "rankvalue", type = Double.class) }) })
@Entity
@Table(name = "taxonomy_definition", indexes = {
		@Index(name = "idx_canonical_form", columnList = "canonical_form, rank, is_deleted") })
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaxonomyDefinition {

	@Schema(description = "Taxonomy unique ID", example = "12345")
	private Long id;

	@Schema(description = "Full binomial form, if applicable", example = "Homo sapiens")
	private String binomialForm;

	@Schema(description = "Canonical (normalized, stripped) form", required = true, example = "Homo sapiens")
	private String canonicalForm;

	@Schema(description = "Italicised form, for web display", required = true, example = "<i>Homo sapiens</i>")
	private String italicisedForm;

	@Schema(description = "External links ID for cross-ref", example = "56789")
	private Long externalLinksId;

	@Schema(description = "Original name as entered", required = true, example = "Homo sapiens Linnaeus, 1758")
	private String name;

	@Schema(description = "Normalized form for searching", example = "homo sapiens")
	private String normalizedForm;

	@Schema(description = "Rank e.g. SPECIES, GENUS", example = "SPECIES")
	private String rank;

	@Schema(description = "Timestamp of upload or update", example = "2024-07-29T10:15:00.0Z")
	private Timestamp uploadTime;

	@Schema(description = "Uploader User ID", example = "99")
	private Long uploaderId;

	@Schema(description = "Nomenclatural status", required = true, example = "ACCEPTED")
	private String status;

	@Schema(description = "Position (e.g. RAW, WORKING)", required = true, example = "RAW")
	private String position;

	@Schema(description = "Authorship and year", example = "Linnaeus, 1758")
	private String authorYear;

	@Schema(description = "External matched database name", example = "GBIF")
	private String matchDatabaseName;

	@Schema(description = "External match ID", example = "247")
	private String matchId;

	@Schema(description = "IBP Source", example = "IBP core")
	private String ibpSource;

	@Schema(description = "Via datasource (provenance)", example = "import")
	private String viaDatasource;

	@Schema(description = "Is flagged in curation?", example = "false")
	private Boolean isFlagged;

	@Schema(description = "Relationship string", example = "synonymOf")
	private String relationship;

	@Schema(description = "Special taxonomic class string", required = true, example = "MAMMALIA")
	private String classs;

	@Schema(description = "Reason for flagging (if any)", example = "Unresolved synonymy")
	private String flaggingReason;

	@Schema(description = "Is this name deleted?", example = "false")
	private Boolean isDeleted;

	@Schema(description = "Reason for being in dirty list", example = "Unparsed author")
	private String dirtyListReason;

	@Schema(description = "Description of curation activity", example = "Imported from legacy system")
	private String activityDescription;

	@Schema(description = "Default hierarchy (JSON or path)", example = "[1,2,3]")
	private String defaultHierarchy;

	@Schema(description = "Source system or provider identifier", example = "GBIF:1234")
	private String nameSourceId;

	public TaxonomyDefinition() {
	}

	@Id
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
