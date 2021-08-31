package com.strandls.taxonomy.pojo;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.FieldResult;
import javax.persistence.Id;
import javax.persistence.SqlResultSetMapping;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.strandls.taxonomy.pojo.response.TaxonRelation;
import com.vladmihalcea.hibernate.type.array.ListArrayType;
import com.vladmihalcea.hibernate.type.json.JsonStringType;

@SqlResultSetMapping(name = "TaxonomyESDocumentMapping", entities = {
		@EntityResult(entityClass = TaxonomyESDocument.class, fields = { @FieldResult(name = "id", column = "id"),
				@FieldResult(name = "name", column = "name"),
				@FieldResult(name = "canonical_form", column = "canonical_form"),
				@FieldResult(name = "italicised_form", column = "italicised_form"),
				@FieldResult(name = "rank", column = "rank"), @FieldResult(name = "status", column = "status"),
				@FieldResult(name = "position", column = "position"), @FieldResult(name = "path", column = "path"),
				@FieldResult(name = "hierarchy", column = "hierarchy"),
				@FieldResult(name = "accepted_ids", column = "accepted_ids"),
				@FieldResult(name = "accepted_names", column = "accepted_names"),
				@FieldResult(name = "common_names", column = "common_names"),
				@FieldResult(name = "group_id", column = "group_id"),
				@FieldResult(name = "group_name", column = "group_name") }) })
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonStringType.class),
		@TypeDef(name = "list-array", typeClass = ListArrayType.class), })

@SqlResultSetMapping(name = "TaxonomyRelation", classes = {
		@ConstructorResult(targetClass = TaxonRelation.class, columns = { @ColumnResult(name = "id", type = Long.class),
				@ColumnResult(name = "name", type = String.class), @ColumnResult(name = "rank", type = String.class),
				@ColumnResult(name = "path", type = String.class),
				@ColumnResult(name = "classification", type = Long.class),
				@ColumnResult(name = "parent", type = Long.class),
				@ColumnResult(name = "position", type = String.class) }) })
@Entity
@JsonIgnoreProperties
public class TaxonomyESDocument {

	@Id
	private Long id;
	private String name;
	private String canonical_form;
	private String italicised_form;
	private String rank;
	private String status;
	private String position;
	private String path;
	private String hierarchy;

	@Type(type = "list-array")
	@Column(columnDefinition = "bigint[]")
	private List<Long> accepted_ids = new ArrayList<>();

	@Type(type = "list-array")
	@Column(columnDefinition = "text[]")
	private List<String> accepted_names = new ArrayList<>();

	@Type(type = "jsonb")
	@Column(columnDefinition = "json")
	private List<JsonNode> common_names = new ArrayList<>();

	private Long group_id;
	private String group_name;

	public TaxonomyESDocument() {
		super();
	}

	public TaxonomyESDocument(Long id, String name, String canonical_form, String italicised_form, String rank,
			String status, String position, String path, String hierarchy, List<Long> accepted_ids,
			List<String> accepted_names, List<JsonNode> common_names, Long group_id, String group_name) {
		super();
		this.id = id;
		this.name = name;
		this.canonical_form = canonical_form;
		this.italicised_form = italicised_form;
		this.rank = rank;
		this.status = status;
		this.position = position;
		this.path = path;
		this.hierarchy = hierarchy;
		this.accepted_ids = accepted_ids;
		this.accepted_names = accepted_names;
		this.common_names = common_names;
		this.group_id = group_id;
		this.group_name = group_name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCanonical_form() {
		return canonical_form;
	}

	public void setCanonical_form(String canonical_form) {
		this.canonical_form = canonical_form;
	}

	public String getItalicised_form() {
		return italicised_form;
	}

	public void setItalicised_form(String italicised_form) {
		this.italicised_form = italicised_form;
	}

	public String getRank() {
		return rank;
	}

	public void setRank(String rank) {
		this.rank = rank;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getHierarchy() {
		return hierarchy;
	}

	public void setHierarchy(String hierarchy) {
		this.hierarchy = hierarchy;
	}

	public List<Long> getAccepted_ids() {
		return accepted_ids;
	}

	public void setAccepted_ids(List<Long> accepted_ids) {
		this.accepted_ids = accepted_ids;
	}

	public List<String> getAccepted_names() {
		return accepted_names;
	}

	public void setAccepted_names(List<String> accepted_names) {
		this.accepted_names = accepted_names;
	}

	public List<JsonNode> getCommon_names() {
		return common_names;
	}

	public void setCommon_names(List<JsonNode> common_names) {
		this.common_names = common_names;
	}

	public Long getGroup_id() {
		return group_id;
	}

	public void setGroup_id(Long group_id) {
		this.group_id = group_id;
	}

	public String getGroup_name() {
		return group_name;
	}

	public void setGroup_name(String group_name) {
		this.group_name = group_name;
	}

}
