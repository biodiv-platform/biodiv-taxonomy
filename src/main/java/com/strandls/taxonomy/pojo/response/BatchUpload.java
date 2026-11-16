package com.strandls.taxonomy.pojo.response;

import java.util.List;
import java.util.Map;

public class BatchUpload {

	private String scientificName;
	private String taxonId;
	private String speciesId;
	private String status;
	private String position;
	private String hierarchy;
	private String action;
	private String error;
	private String acceptedId;
	private String rank;
	private Map<String, String> updateHier;
	/** From the Given Names sheet "TargetNameSourceId" column (→ taxonomy_definition.name_source_id) */
	private String targetNameSourceId;
	/** From the Given Names sheet "TargetViaDatasource" column (→ taxonomy_definition.via_datasource) */
	private String targetViaDatasource;
	/** From the Given Names sheet "TargetMatchId" column (→ taxonomy_definition.match_id) */
	private String targetMatchId;
	/** From the Given Names sheet "TargetMatchDatabaseName" column (→ taxonomy_definition.match_database_name) */
	private String targetMatchDatabaseName;

	private List<String> synonyms;
	private List<String> commonNames;
	private List<String> positionHier;

	public BatchUpload() {
		super();
	}

	public String getScientificName() {
		return scientificName;
	}

	public void setScientificName(String scientificName) {
		this.scientificName = scientificName;
	}

	public String getRank() {
		return rank;
	}

	public void setRank(String rank) {
		this.rank = rank;
	}

	public String getTaxonId() {
		return taxonId;
	}

	public void setTaxonId(String taxonId) {
		this.taxonId = taxonId;
	}

	public String getAcceptedId() {
		return acceptedId;
	}

	public void setAcceptedId(String acceptedId) {
		this.acceptedId = acceptedId;
	}

	public String getSpeciesId() {
		return speciesId;
	}

	public void setSpeciesId(String speciesId) {
		this.speciesId = speciesId;
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

	public String getHierarchy() {
		return hierarchy;
	}

	public void setHierarchy(String hierarchy) {
		this.hierarchy = hierarchy;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public List<String> getSynonyms() {
		return synonyms;
	}

	public void setSynonyms(List<String> synonyms) {
		this.synonyms = synonyms;
	}

	public List<String> getCommonNames() {
		return commonNames;
	}

	public void setCommonNames(List<String> commonNames) {
		this.commonNames = commonNames;
	}
	
	public List<String> getPositionHier() {
		return positionHier;
	}

	public void setPositionHier(List<String> positionHier) {
		this.positionHier = positionHier;
	}
	
	public Map<String, String> getUpdateHier() {
		return updateHier;
	}

	public void setUpdateHier(Map<String, String> updateHier) {
		this.updateHier = updateHier;
	}

	public String getTargetNameSourceId() {
		return targetNameSourceId;
	}

	public void setTargetNameSourceId(String targetNameSourceId) {
		this.targetNameSourceId = targetNameSourceId;
	}

	public String getTargetViaDatasource() {
		return targetViaDatasource;
	}

	public void setTargetViaDatasource(String targetViaDatasource) {
		this.targetViaDatasource = targetViaDatasource;
	}

	public String getTargetMatchId() {
		return targetMatchId;
	}

	public void setTargetMatchId(String targetMatchId) {
		this.targetMatchId = targetMatchId;
	}

	public String getTargetMatchDatabaseName() {
		return targetMatchDatabaseName;
	}

	public void setTargetMatchDatabaseName(String targetMatchDatabaseName) {
		this.targetMatchDatabaseName = targetMatchDatabaseName;
	}
}