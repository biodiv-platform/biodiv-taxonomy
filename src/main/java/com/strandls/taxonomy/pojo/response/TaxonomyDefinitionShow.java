package com.strandls.taxonomy.pojo.response;

import java.util.List;

import com.strandls.taxonomy.pojo.CommonName;
import com.strandls.taxonomy.pojo.TaxonomyDefinition;

public class TaxonomyDefinitionShow {

	private TaxonomyDefinition taxonomyDefinition;
	private List<BreadCrumb> hierarchy;
	private List<TaxonomyDefinition> acceptedNames;
	private List<TaxonomyDefinition> synonymNames;
	private List<CommonName> commonNames;

	public TaxonomyDefinitionShow() {
		super();
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

	public List<TaxonomyDefinition> getAcceptedNames() {
		return acceptedNames;
	}

	public void setAcceptedNames(List<TaxonomyDefinition> acceptedNames) {
		this.acceptedNames = acceptedNames;
	}

	public List<TaxonomyDefinition> getSynonymNames() {
		return synonymNames;
	}

	public void setSynonymNames(List<TaxonomyDefinition> synonymNames) {
		this.synonymNames = synonymNames;
	}

	public List<CommonName> getCommonNames() {
		return commonNames;
	}

	public void setCommonNames(List<CommonName> commonNames) {
		this.commonNames = commonNames;
	}

}
