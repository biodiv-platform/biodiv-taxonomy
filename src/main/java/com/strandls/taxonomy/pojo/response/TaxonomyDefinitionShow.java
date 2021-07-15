package com.strandls.taxonomy.pojo.response;

import java.util.List;

import com.strandls.taxonomy.pojo.TaxonomyDefinition;

public class TaxonomyDefinitionShow {

	private TaxonomyDefinition taxonomyDefinition;
	private List<BreadCrumb> hierarchy;
	private List<BreadCrumb> acceptedNames;
	private List<BreadCrumb> synonymNames;

	public TaxonomyDefinitionShow() {
		super();
	}

	public TaxonomyDefinitionShow(TaxonomyDefinition taxonomyDefinition, List<BreadCrumb> hierarchy,
			List<BreadCrumb> acceptedNames, List<BreadCrumb> synonymNames) {
		super();
		this.taxonomyDefinition = taxonomyDefinition;
		this.hierarchy = hierarchy;
		this.acceptedNames = acceptedNames;
		this.synonymNames = synonymNames;
	}

	public TaxonomyDefinition getTaxonomyDefinition() {
		return taxonomyDefinition;
	}

	public void setTaxonomyDefinition(TaxonomyDefinition taxonomyDefinition) {
		this.taxonomyDefinition = taxonomyDefinition;
	}

	public List<BreadCrumb> getHierarchy() {
		return hierarchy;
	}

	public void setHierarchy(List<BreadCrumb> hierarchy) {
		this.hierarchy = hierarchy;
	}

	public List<BreadCrumb> getAcceptedNames() {
		return acceptedNames;
	}

	public void setAcceptedNames(List<BreadCrumb> acceptedNames) {
		this.acceptedNames = acceptedNames;
	}

	public List<BreadCrumb> getSynonymNames() {
		return synonymNames;
	}

	public void setSynonymNames(List<BreadCrumb> synonymNames) {
		this.synonymNames = synonymNames;
	}

}
