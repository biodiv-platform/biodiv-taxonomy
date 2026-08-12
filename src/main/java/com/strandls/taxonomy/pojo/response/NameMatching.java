package com.strandls.taxonomy.pojo.response;

import java.util.List;
import java.util.Map;

public class NameMatching {

	private List<String> headers;
	private List<Map<String, Object>> data;
	private Map<String, Object> synonym;
	private Map<String, Object> cname;
	private Map<String, Object> hierarchy;

	public NameMatching() {
		super();
	}

	/**
	 * @param headers
	 */
	public NameMatching(List<String> headers, List<Map<String, Object>> data, Map<String, Object> synonym,
			Map<String, Object> cname, Map<String, Object> hierarchy) {
		super();
		this.headers = headers;
		this.data = data;
		this.synonym = synonym;
		this.cname = cname;
		this.hierarchy = hierarchy;
	}

	public List<String> getHeaders() {
		return headers;
	}

	public void setHeaders(List<String> headers) {
		this.headers = headers;
	}

	public List<Map<String, Object>> getData() {
		return data;
	}

	public void setData(List<Map<String, Object>> data) {
		this.data = data;
	}

	public Map<String, Object> getSynonym() {
		return synonym;
	}

	public void setSynonym(Map<String, Object> synonym) {
		this.synonym = synonym;
	}

	public Map<String, Object> getCname() {
		return cname;
	}

	public void setCname(Map<String, Object> cname) {
		this.cname = cname;
	}

	public Map<String, Object> getHierarchy() {
		return hierarchy;
	}

	public void setHierarchy(Map<String, Object> hierarchy) {
		this.hierarchy = hierarchy;
	}
}
