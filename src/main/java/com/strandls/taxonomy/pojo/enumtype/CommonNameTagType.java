package com.strandls.taxonomy.pojo.enumtype;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;

@XmlType(name = "taxonomyStatus")
@XmlEnum
public enum CommonNameTagType {
	@XmlEnumValue("name")
	name("name"), @XmlEnumValue("threeLetterCode")
	threeLetterCode("threeLetterCode"), @XmlEnumValue("twoLetterCode")
	twoLetterCode("twoLetterCode");

	private String value;

	private CommonNameTagType(String value) {
		this.value = value;
	}

	public static CommonNameTagType fromValue(String value) {
		for (CommonNameTagType layerStatus : CommonNameTagType.values()) {
			if (layerStatus.value.equals(value))
				return layerStatus;
		}
		throw new IllegalArgumentException(value);
	}
}
