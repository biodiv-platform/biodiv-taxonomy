/** */
package com.strandls.taxonomy.pojo.response;

import java.util.List;

/**
 * @author Abhishek Rudra
 */
public class TaxonTree {

	private Long taxonId;
	private List<Long> taxonList;

	/** */
	public TaxonTree() {
		super();
	}

	/**
	 * @param taxonId
	 * @param taxonList
	 */
	public TaxonTree(Long taxonId, List<Long> taxonList) {
		super();
		this.taxonId = taxonId;
		this.taxonList = taxonList;
	}

	public Long getTaxonId() {
		return taxonId;
	}

	public void setTaxonId(Long taxonId) {
		this.taxonId = taxonId;
	}

	public List<Long> getTaxonList() {
		return taxonList;
	}

	public void setTaxonList(List<Long> taxonList) {
		this.taxonList = taxonList;
	}
}
