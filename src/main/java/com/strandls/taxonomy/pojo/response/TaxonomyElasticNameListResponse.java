package com.strandls.taxonomy.pojo.response;

import java.util.List;

import com.strandls.taxonomy.util.TaxonomyListMinimalData;

public class TaxonomyElasticNameListResponse {

	private Integer count;
	private List<TaxonomyListMinimalData> taxonomyNameListItems;
	private String acceptedPath;
	private Long synonymId;

	public TaxonomyElasticNameListResponse() {
		super();
	}

	public TaxonomyElasticNameListResponse(Integer count, List<TaxonomyListMinimalData> taxonomyNameListItems,
			String acceptedPath, Long synonymId) {
		super();
		this.count = count;
		this.taxonomyNameListItems = taxonomyNameListItems;
		this.acceptedPath = acceptedPath;
		this.synonymId = synonymId;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public List<TaxonomyListMinimalData> getTaxonomyNameListItems() {
		return taxonomyNameListItems;
	}

	public void setTaxonomyNameListItems(List<TaxonomyListMinimalData> taxonomyNameListItems) {
		this.taxonomyNameListItems = taxonomyNameListItems;
	}

	public String getAcceptedPath() {
		return acceptedPath;
	}

	public void setAcceptedPath(String acceptedPath) {
		this.acceptedPath = acceptedPath;
	}

	public Long getSynonymId() {
		return synonymId;
	}

	public void setSynonymId(Long synonymId) {
		this.synonymId = synonymId;
	}
}
