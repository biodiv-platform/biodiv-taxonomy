package com.strandls.taxonomy.pojo.response;
import java.util.List;

import com.strandls.taxonomy.util.TaxonomyListMinimalData;

public class TaxonomyElasticNameListResponse {

	private Integer count;
	private List<TaxonomyListMinimalData> taxonomyNameListItems;

	public TaxonomyElasticNameListResponse() {
		super();
	}

	public TaxonomyElasticNameListResponse(Integer count, List<TaxonomyListMinimalData> taxonomyNameListItems) {
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

	public List<TaxonomyListMinimalData> getTaxonomyNameListItems() {
		return taxonomyNameListItems;
	}

	public void setTaxonomyNameListItems(List<TaxonomyListMinimalData> taxonomyNameListItems) {
		this.taxonomyNameListItems = taxonomyNameListItems;
	}
}
