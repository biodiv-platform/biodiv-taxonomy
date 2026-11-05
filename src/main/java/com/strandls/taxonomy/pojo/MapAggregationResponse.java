package com.strandls.taxonomy.pojo;

import java.util.Map;

public class MapAggregationResponse {

	private Map<String, Long> groupStatus;
	private Map<String, Long> groupRank;
	private Map<String, Long> groupPosition;

	public Map<String, Long> getGroupStatus() {
		return groupStatus;
	}

	public void setGroupStatus(Map<String, Long> groupStatus) {
		this.groupStatus = groupStatus;
	}
	
	public Map<String, Long> getGroupPosition() {
		return groupPosition;
	}

	public void setGroupPosition(Map<String, Long> groupPosition) {
		this.groupPosition = groupPosition;
	}

	public Map<String, Long> getGroupRank() {
		return groupRank;
	}

	public void setGroupRank(Map<String, Long> groupRank) {
		this.groupRank = groupRank;
	}

}