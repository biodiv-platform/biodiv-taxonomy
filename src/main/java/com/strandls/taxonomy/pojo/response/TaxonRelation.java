package com.strandls.taxonomy.pojo.response;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public class TaxonRelation {

	private Long taxonid;
	private Long id;
	private String text;
	private String rank;
	private String path;
	private Long classification;
	private Long parent;
	private String position;

	private Long speciesId;
	private List<String> ids;

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<TaxonRelation> children = new ArrayList<>();

	public TaxonRelation() {
		super();
	}

	public TaxonRelation(Long id, String text, String rank, String path, Long classification, Long parent,
			String position) {
		super();
		this.id = id;
		this.taxonid = id;
		this.text = text;
		this.rank = rank;
		this.path = path;
		this.classification = classification;
		this.parent = parent;
		this.position = position;
	}

	public void addChild(TaxonRelation taxonRelation) {
		this.children.add(taxonRelation);
	}

	public TaxonRelation(Long taxonid, Long id, String text, String rank, String path, Long classification, Long parent,
			String position, Long speciesId, List<String> ids, List<TaxonRelation> children) {
		super();
		this.taxonid = taxonid;
		this.id = id;
		this.text = text;
		this.rank = rank;
		this.path = path;
		this.classification = classification;
		this.parent = parent;
		this.position = position;
		this.speciesId = speciesId;
		this.ids = ids;
		this.children = children;
	}

	public Long getTaxonid() {
		return taxonid;
	}

	public void setTaxonid(Long taxonid) {
		this.taxonid = taxonid;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getRank() {
		return rank;
	}

	public void setRank(String rank) {
		this.rank = rank;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Long getClassification() {
		return classification;
	}

	public void setClassification(Long classification) {
		this.classification = classification;
	}

	public Long getParent() {
		return parent;
	}

	public void setParent(Long parent) {
		this.parent = parent;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public Long getSpeciesId() {
		return speciesId;
	}

	public void setSpeciesId(Long speciesId) {
		this.speciesId = speciesId;
	}

	public List<String> getIds() {
		return ids;
	}

	public void setIds(List<String> ids) {
		this.ids = ids;
	}

	public List<TaxonRelation> getChildren() {
		return children;
	}

	public void setChildren(List<TaxonRelation> children) {
		this.children = children;
	}
}
