package com.strandls.taxonomy.pojo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 
 * @author vilay
 *
 */

@Entity
@Table(name = "taxonomy_rank")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Rank {

	private Long id;
	private String name;
	private Double rankValue;
	private Boolean isRequired;
	private Boolean isDeleted;

	public Rank() {
		super();
	}

	public Rank(Long id, String name, Double rankValue, Boolean isRequired, Boolean isDeleted) {
		super();
		this.id = id;
		this.name = name;
		this.rankValue = rankValue;
		this.isRequired = isRequired;
		this.isDeleted = isDeleted;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rank_id_generator")
	@SequenceGenerator(name = "rank_id_generator", sequenceName = "rank_id_seq", allocationSize = 1)
	@Column(name = "id")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Double getRankValue() {
		return rankValue;
	}

	public void setRankValue(Double rankValue) {
		this.rankValue = rankValue;
	}

	public Boolean getIsRequired() {
		return isRequired;
	}

	public void setIsRequired(Boolean isRequired) {
		this.isRequired = isRequired;
	}

	public Boolean getIsDeleted() {
		return isDeleted;
	}

	public void setIsDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
	}
}
