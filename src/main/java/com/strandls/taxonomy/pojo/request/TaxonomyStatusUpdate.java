package com.strandls.taxonomy.pojo.request;

import java.util.List;
import java.util.Map;

import com.strandls.taxonomy.pojo.enumtype.TaxonomyStatus;

/**
 * @author vilay
 * @param taxonId        - Taxonomy id which need to be updated
 * @param taxonomyStatus - taxonomy status {@code TaxonomyStatus} to be updated
 * @param hierarchy      - This will be null for the accepted name to synonym
 *                       change
 * @param newTaxonId     - This will be null for the synonym to accepted name
 *                       change.
 * @return
 */
public class TaxonomyStatusUpdate {

	private Long taxonId;
	private TaxonomyStatus status;
	private List<Long> newTaxonId;
	private Map<String, String> hierarchy;

	public TaxonomyStatusUpdate() {
		super();
	}

	public TaxonomyStatusUpdate(Long taxonId, TaxonomyStatus status, List<Long> newTaxonId,
			Map<String, String> hierarchy) {
		super();
		this.taxonId = taxonId;
		this.status = status;
		this.newTaxonId = newTaxonId;
		this.hierarchy = hierarchy;
	}

	public Long getTaxonId() {
		return taxonId;
	}

	public void setTaxonId(Long taxonId) {
		this.taxonId = taxonId;
	}

	public TaxonomyStatus getStatus() {
		return status;
	}

	public void setStatus(TaxonomyStatus status) {
		this.status = status;
	}

	public List<Long> getNewTaxonId() {
		return newTaxonId;
	}

	public void setNewTaxonId(List<Long> newTaxonId) {
		this.newTaxonId = newTaxonId;
	}

	public Map<String, String> getHierarchy() {
		return hierarchy;
	}

	public void setHierarchy(Map<String, String> hierarchy) {
		this.hierarchy = hierarchy;
	}
}
