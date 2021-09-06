/**
 * 
 */
package com.strandls.taxonomy.pojo;

/**
 * @author Abhishek Rudra
 *
 * 
 */
public class CommonNamesData {

	private Long id;
	private Long languageId;
	private String name;
	private Long taxonConceptId;

	/**
	 * 
	 */
	public CommonNamesData() {
		super();
	}

	/**
	 * @param id
	 * @param languageId
	 * @param name
	 * @param taxonConceptId
	 */
	public CommonNamesData(Long id, Long languageId, String name, Long taxonConceptId) {
		super();
		this.id = id;
		this.languageId = languageId;
		this.name = name;
		this.taxonConceptId = taxonConceptId;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getLanguageId() {
		return languageId;
	}

	public void setLanguageId(Long languageId) {
		this.languageId = languageId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getTaxonConceptId() {
		return taxonConceptId;
	}

	public void setTaxonConceptId(Long taxonConceptId) {
		this.taxonConceptId = taxonConceptId;
	}

}
