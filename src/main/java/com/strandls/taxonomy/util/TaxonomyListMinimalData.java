package com.strandls.taxonomy.util;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Mekala Rishitha Ravi
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaxonomyListMinimalData {

	@JsonAlias("position")
	private String position;

	public TaxonomyListMinimalData() {
	}

	public TaxonomyListMinimalData(String position) {
		this.position = position;
	}

	public String getPosition() {
		return position;
	}

	public void setSpeciesGroup(String position) {
		this.position = position;
	}

}
