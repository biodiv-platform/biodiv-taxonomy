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
	private String position;

	/**
	 * @param id
	 * @param name
	 */
	public BreadCrumb(Long id, String name, String rankName, String position) {
		super();
		this.id = id;
		this.name = name;
		this.rankName = rankName;
		this.position = position;
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
	
	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

}
