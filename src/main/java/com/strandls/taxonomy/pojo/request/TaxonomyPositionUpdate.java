package com.strandls.taxonomy.pojo.request;

import com.strandls.taxonomy.pojo.enumtype.TaxonomyPosition;

/**
 * 
 * @author vilay
 *
 * @return
 */

public class TaxonomyPositionUpdate {

	private Long taxonId;
	private TaxonomyPosition position;

	public TaxonomyPositionUpdate() {
		super();
	}

	public TaxonomyPositionUpdate(Long taxonId, TaxonomyPosition position) {
		super();
		this.taxonId = taxonId;
		this.position = position;
	}

	public Long getTaxonId() {
		return taxonId;
	}

	public void setTaxonId(Long taxonId) {
		this.taxonId = taxonId;
	}

	public TaxonomyPosition getPosition() {
		return position;
	}

	public void setPosition(TaxonomyPosition position) {
		this.position = position;
	}

}
