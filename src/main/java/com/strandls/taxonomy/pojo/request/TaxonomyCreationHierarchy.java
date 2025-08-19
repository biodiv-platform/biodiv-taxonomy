package com.strandls.taxonomy.pojo.request;

import java.util.LinkedHashMap;
import java.util.Map;

import com.strandls.utility.pojo.ParsedName;

public class TaxonomyCreationHierarchy {

	private Long matchedTaxonId;
	private String matchedRank;
	// Rank to scientific name
	private Map<String, ParsedName> unmatchedNodeToCreate;

	public TaxonomyCreationHierarchy() {
		super();
		unmatchedNodeToCreate = new LinkedHashMap<>();
	}

	public Long getMatchedTaxonId() {
		return matchedTaxonId;
	}

	public void setMatchedTaxonId(Long matchedTaxonId) {
		this.matchedTaxonId = matchedTaxonId;
	}

	public String getMatchedRank() {
		return matchedRank;
	}

	public void setMatchedRank(String matchedRank) {
		this.matchedRank = matchedRank;
	}

	public Map<String, ParsedName> getUnmatchedNodeToCreate() {
		return unmatchedNodeToCreate;
	}

	public void setUnmatchedNodeToCreate(Map<String, ParsedName> unmatchedNodeToCreate) {
		this.unmatchedNodeToCreate = unmatchedNodeToCreate;
	}

	public void addUnmatchedNodeToCreate(String key, ParsedName value) {
		this.unmatchedNodeToCreate.put(key, value);
	}
}
