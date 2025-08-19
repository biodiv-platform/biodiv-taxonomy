package com.strandls.taxonomy.pojo.enumtype;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;

@XmlType(name = "taxonomyStatus")
@XmlEnum
public enum TaxonomyStatus {
	@XmlEnumValue("ACCEPTED")
	ACCEPTED("ACCEPTED"), @XmlEnumValue("SYNONYM")
	SYNONYM("SYNONYM");

	private String value;

	private TaxonomyStatus(String value) {
		this.value = value;
	}

	public static TaxonomyStatus fromValue(String value) {
		for (TaxonomyStatus layerStatus : TaxonomyStatus.values()) {
			if (layerStatus.value.equals(value))
				return layerStatus;
		}
		throw new IllegalArgumentException(value);
	}

	public static List<String> getAllOrSpecified(String statusListString) {
		List<String> statusList = new ArrayList<>();
		if (statusListString == null || "".equals(statusListString)) {
			for (TaxonomyStatus status : TaxonomyStatus.values()) {
				statusList.add(status.name());
			}
		} else {
			for (String status : statusListString.split(",")) {
				status = status.toUpperCase().trim();
				status = TaxonomyStatus.fromValue(status).name();
				statusList.add(status);
			}
		}
		return statusList;
	}
}
