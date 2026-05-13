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
	
	@JsonAlias("id")
	private Long id;
	
	@JsonAlias("name")
	private String name;
	
	@JsonAlias("rank")
	private String rank;
	
	@JsonAlias("status")
	private String status;

	public TaxonomyListMinimalData() {
	}

	public TaxonomyListMinimalData(String position, Long id, String name, String rank, String status) {
		this.position = position;
		this.id = id;
		this.name = name;
		this.rank = rank;
		this.status = status;
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getRank() {
		return rank;
	}

	public void setRank(String rank) {
		this.rank = rank;
	}
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
