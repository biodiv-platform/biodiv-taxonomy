package com.strandls.taxonomy.pojo.response;

import java.util.List;

public class TaxonomyNameListResponse {

	private Integer count;
	private List<TaxonomyNamelistItem> taxonomyNameListItems;

	public TaxonomyNameListResponse() {
		super();
	}

	public TaxonomyNameListResponse(Integer count, List<TaxonomyNamelistItem> taxonomyNameListItems) {
		super();
		this.count = count;
		this.taxonomyNameListItems = taxonomyNameListItems;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public List<TaxonomyNamelistItem> getTaxonomyNameListItems() {
		return taxonomyNameListItems;
	}

	public void setTaxonomyNameListItems(List<TaxonomyNamelistItem> taxonomyNameListItems) {
		this.taxonomyNameListItems = taxonomyNameListItems;
	}
}
