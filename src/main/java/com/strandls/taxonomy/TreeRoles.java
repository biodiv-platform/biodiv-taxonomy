/** */
package com.strandls.taxonomy;

/**
 * @author Abhishek Rudra
 */
public enum TreeRoles {
	OBSERVATIONCURATOR("OBSERVATION CURATOR"), SPECIESCONTRIBUTOR("SPECIES CONTRIBUTOR"),
	TAXONOMYCONTRIBUTOR("TAXONOMY CONTRIBUTOR");

	String value;

	private TreeRoles(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
