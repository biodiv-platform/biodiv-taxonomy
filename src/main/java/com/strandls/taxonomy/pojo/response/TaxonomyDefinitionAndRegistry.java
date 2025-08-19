package com.strandls.taxonomy.pojo.response;

import java.util.List;

import com.strandls.taxonomy.pojo.TaxonomyDefinition;

public class TaxonomyDefinitionAndRegistry {

	private TaxonomyDefinition taxonomyDefinition;
	private List<TaxonomyRegistryResponse> registry;

	public TaxonomyDefinitionAndRegistry() {
		super();
	}

	public TaxonomyDefinitionAndRegistry(TaxonomyDefinition taxonomyDefinition,
			List<TaxonomyRegistryResponse> registry) {
		super();
		this.taxonomyDefinition = taxonomyDefinition;
		this.registry = registry;
	}

	public TaxonomyDefinition getTaxonomyDefinition() {
		return taxonomyDefinition;
	}

	public void setTaxonomyDefinition(TaxonomyDefinition taxonomyDefinition) {
		this.taxonomyDefinition = taxonomyDefinition;
	}

	public List<TaxonomyRegistryResponse> getRegistry() {
		return registry;
	}

	public void setRegistry(List<TaxonomyRegistryResponse> registry) {
		this.registry = registry;
	}
}
