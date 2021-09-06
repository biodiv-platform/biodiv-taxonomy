/**
 * 
 */
package com.strandls.taxonomy.pojo;

/**
 * @author Abhishek Rudra
 *
 * 
 */
public class SynonymData {

	private Long id;
	private String name;
	private String rank;
	private String dataSource;
	private String dataSourceId;

	/**
	 * 
	 */
	public SynonymData() {
		super();
	}

	/**
	 * @param id
	 * @param name
	 * @param rank
	 * @param dataSource
	 * @param dataSourceId
	 */
	public SynonymData(Long id, String name, String rank, String dataSource, String dataSourceId) {
		super();
		this.id = id;
		this.name = name;
		this.rank = rank;
		this.dataSource = dataSource;
		this.dataSourceId = dataSourceId;
	}

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

	public String getRank() {
		return rank;
	}

	public void setRank(String rank) {
		this.rank = rank;
	}

	public String getDataSource() {
		return dataSource;
	}

	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	public void setDataSourceId(String dataSourceId) {
		this.dataSourceId = dataSourceId;
	}

}
