package com.strandls.taxonomy.pojo.enumtype;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "taxonomyStatus")
@XmlEnum
public enum TaxonomyPosition {

	@XmlEnumValue("CLEAN")
	CLEAN("CLEAN"),
	@XmlEnumValue("RAW")
	RAW("RAW"),
	@XmlEnumValue("WORKING")
	WORKING("WORKING");

	private String value;
	
	private TaxonomyPosition(String value) {
		this.value = value;
	}
	
	public static TaxonomyPosition fromValue(String value) {
		for(TaxonomyPosition layerStatus : TaxonomyPosition.values()) {
			if(layerStatus.value.equals(value))
				return layerStatus;
		}
		throw new IllegalArgumentException(value);
	}
	
	public static List<String> getAllOrSpecified(String positionListString) {
		List<String> positionList = new ArrayList<>();
		if (positionListString == null || "".equals(positionListString)) {
			for (TaxonomyPosition position : TaxonomyPosition.values()) {
				positionList.add(position.name());
			}
		} else {
			for (String position : positionListString.split(",")) {
				position = position.toUpperCase().trim();
				position = TaxonomyPosition.fromValue(position).name();
				positionList.add(position);
			}
		}
		return positionList;
	}
}
