package com.strandls.taxonomy.pojo.response;

import java.util.List;

public class TaxonomySearch {

	private List<TaxonomyDefinitionAndRegistry> matched;
	private List<TaxonomyDefinitionAndRegistry> parentMatched;

	public TaxonomySearch() {
		super();
	}

	public TaxonomySearch(List<TaxonomyDefinitionAndRegistry> matched,
			List<TaxonomyDefinitionAndRegistry> parentMatched) {
		super();
		this.matched = matched;
		this.parentMatched = parentMatched;
	}

	public List<TaxonomyDefinitionAndRegistry> getMatched() {
		return matched;
	}

	public void setMatched(List<TaxonomyDefinitionAndRegistry> matched) {
		this.matched = matched;
	}

	public List<TaxonomyDefinitionAndRegistry> getParentMatched() {
		return parentMatched;
	}

	public void setParentMatched(List<TaxonomyDefinitionAndRegistry> parentMatched) {
		this.parentMatched = parentMatched;
	}
}
