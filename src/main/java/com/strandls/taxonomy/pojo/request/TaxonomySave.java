package com.strandls.taxonomy.pojo.request;

import java.util.Map;

import com.strandls.taxonomy.pojo.enumtype.TaxonomyPosition;
import com.strandls.taxonomy.pojo.enumtype.TaxonomyStatus;

public class TaxonomySave {

	private String scientificName;
	private String rank;
	private String synonyms;
	private TaxonomyStatus status;
	private TaxonomyPosition position;
	private String sourceId;
	private String source;
	private Map<Long, String[]> commonNames;

	/**
	 * This two are based on the taxonomy status acceptedId is required if the
	 * status is synonym. rankToName is required if the status is accepted.
	 */
	private Long acceptedId;

	private Map<String, String> rankToName;

	public TaxonomySave() {
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

	public String getSynonyms() {
		return synonyms;
	}

	public void setSynonyms(String synonyms) {
		this.synonyms = synonyms;
	}

	public TaxonomyStatus getStatus() {
		return status;
	}

	public void setStatus(TaxonomyStatus status) {
		this.status = status;
	}

	public TaxonomyPosition getPosition() {
		return position;
	}

	public void setPosition(TaxonomyPosition position) {
		this.position = position;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public Map<Long, String[]> getCommonNames() {
		return commonNames;
	}

	public void setCommonNames(Map<Long, String[]> commonNames) {
		this.commonNames = commonNames;
	}

	public Long getAcceptedId() {
		return acceptedId;
	}

	public void setAcceptedId(Long acceptedId) {
		this.acceptedId = acceptedId;
	}

	public Map<String, String> getRankToName() {
		return rankToName;
	}

	public void setRankToName(Map<String, String> rankToName) {
		this.rankToName = rankToName;
	}
}
