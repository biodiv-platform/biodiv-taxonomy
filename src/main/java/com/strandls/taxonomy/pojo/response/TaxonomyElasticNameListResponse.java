package com.strandls.taxonomy.pojo.response;

import java.util.List;

import com.strandls.taxonomy.pojo.MapAggregationResponse;
import com.strandls.taxonomy.util.TaxonomyListMinimalData;

public class TaxonomyElasticNameListResponse {

	private Long count;
	private List<TaxonomyListMinimalData> taxonomyNameListItems;
	private String acceptedPath;
	private Long synonymId;
	private MapAggregationResponse aggregationData;

	public TaxonomyElasticNameListResponse() {
		super();
	}

	public TaxonomyElasticNameListResponse(Long count, List<TaxonomyListMinimalData> taxonomyNameListItems,
			String acceptedPath, Long synonymId, MapAggregationResponse aggregationData) {
		super();
		this.count = count;
		this.taxonomyNameListItems = taxonomyNameListItems;
		this.acceptedPath = acceptedPath;
		this.synonymId = synonymId;
		this.aggregationData = aggregationData;
	}

	public Long getCount() {
		return count;
	}

	public void setCount(Long totalCount) {
		this.count = totalCount;
	}
	
	public MapAggregationResponse getAggregationData() {
		return aggregationData;
	}

	public void setAggregationData(MapAggregationResponse aggregationData) {
		this.aggregationData = aggregationData;
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
