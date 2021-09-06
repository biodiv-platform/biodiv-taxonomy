/**
 * 
 */
package com.strandls.taxonomy.pojo;

import java.util.List;

/**
 * @author Abhishek Rudra
 *
 * 
 */
public class TaxonomicNames {

	private List<CommonName> commonNames;
	private List<TaxonomyDefinition> synonyms;

	/**
	 * 
	 */
	public TaxonomicNames() {
		super();
	}

	/**
	 * @param commonNames
	 * @param synonyms
	 */
	public TaxonomicNames(List<CommonName> commonNames, List<TaxonomyDefinition> synonyms) {
		super();
		this.commonNames = commonNames;
		this.synonyms = synonyms;
	}

	public List<CommonName> getCommonNames() {
		return commonNames;
	}

	public void setCommonNames(List<CommonName> commonNames) {
		this.commonNames = commonNames;
	}

	public List<TaxonomyDefinition> getSynonyms() {
		return synonyms;
	}

	public void setSynonyms(List<TaxonomyDefinition> synonyms) {
		this.synonyms = synonyms;
	}

}
