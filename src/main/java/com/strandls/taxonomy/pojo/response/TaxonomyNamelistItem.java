package com.strandls.taxonomy.pojo.response;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.SqlResultSetMapping;

@SqlResultSetMapping(
	name = "TaxonomyNamelistItemMapping",
	classes = @ConstructorResult(
		targetClass = TaxonomyNamelistItem.class,
		columns = {
			@ColumnResult(name = "id", type = Long.class),
			@ColumnResult(name = "name", type = String.class),
			@ColumnResult(name = "rank", type = String.class),
			@ColumnResult(name = "path", type = String.class),
			@ColumnResult(name = "classification", type = Long.class),
			@ColumnResult(name = "parent", type = String.class),
			@ColumnResult(name = "position", type = String.class)
		}
	)
)
public class TaxonomyNamelistItem {

	private Long id;
	private String rank;
	private String name;
	private String status;
	private String position;
	private Double rankValue;

	public TaxonomyNamelistItem() {
		super();
	}

	public TaxonomyNamelistItem(Long id, String name, String rank, String path, Long classification, String parent, String position) {
		super();
		this.id = id;
		this.name = name;
		this.rank = rank;
		this.status = path;
		this.position = position;
		this.rankValue = null;
	}

	public TaxonomyNamelistItem(Long id, String rank, String name, String status, String position, Double rankValue) {
		super();
		this.id = id;
		this.rank = rank;
		this.name = name;
		this.status = status;
		this.position = position;
		this.rankValue = rankValue;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public Double getRankValue() {
		return rankValue;
	}

	public void setRankValue(Double rankValue) {
		this.rankValue = rankValue;
	}
}
