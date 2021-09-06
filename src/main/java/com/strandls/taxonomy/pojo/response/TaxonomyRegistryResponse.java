package com.strandls.taxonomy.pojo.response;

public class TaxonomyRegistryResponse {

	private String id;
	private String rank;
	private String name;
	private String canonicalForm;

	public TaxonomyRegistryResponse() {
		super();
	}

	public TaxonomyRegistryResponse(String id, String rank, String name, String canonicalForm) {
		super();
		this.id = id;
		this.rank = rank;
		this.name = name;
		this.canonicalForm = canonicalForm;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRank() {
		return rank;
	}

	public void setRank(String rank) {
		this.rank = rank;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCanonicalForm() {
		return canonicalForm;
	}

	public void setCanonicalForm(String canonicalForm) {
		this.canonicalForm = canonicalForm;
	}

}
