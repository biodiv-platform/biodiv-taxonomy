/**
 * 
 */
package com.strandls.taxonomy.pojo.response;

/**
 * @author Abhishek Rudra
 *
 */
public class BreadCrumb {

	private Long id;
	private String name;
	private String rankName;

	/**
	 * @param id
	 * @param name
	 */
	public BreadCrumb(Long id, String name, String rankName) {
		super();
		this.id = id;
		this.name = name;
		this.rankName = rankName;
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

	public String getRankName() {
		return rankName;
	}

	public void setRankName(String rankName) {
		this.rankName = rankName;
	}

}
