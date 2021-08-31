package com.strandls.taxonomy.pojo.request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.NoResultException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.strandls.taxonomy.pojo.enumtype.CommonNameTagType;
import com.strandls.taxonomy.pojo.enumtype.TaxonomyPosition;
import com.strandls.taxonomy.pojo.enumtype.TaxonomyStatus;
import com.strandls.utility.ApiException;
import com.strandls.utility.controller.UtilityServiceApi;
import com.strandls.utility.pojo.Language;

public class FileMetadata {

	private String fileType;
	private Map<String, String> nameToRank;

	private String scientificColumnName;
	private String synonymColumnName;
	private String rankColumnName;
	private String statusColumnName;
	private String positionColumnName;
	private String sourceColumnName;
	private String sourceIdColumnName;

	private CommonNameTagType commonNameTagType;
	private String commonNameColumn;

	@JsonIgnore
	private int scientificColumnIndex = -1;
	@JsonIgnore
	private int synonymColumnIndex = -1;
	@JsonIgnore
	private int rankColumnIndex = -1;
	@JsonIgnore
	private int statusColumnIndex = -1;
	@JsonIgnore
	private int positionColumnIndex = -1;
	@JsonIgnore
	private int sourceColumnIndex = -1;
	@JsonIgnore
	private int sourceIdColumnIndex = -1;
	@JsonIgnore
	private int commonNameColumnIndex = -1;
	@JsonIgnore
	private Map<String, Integer> rankToIndex;

	public FileMetadata() {
		super();
		this.rankToIndex = new HashMap<>();
	}

	public void updateIndices(String[] headers) {
		if (commonNameTagType == null)
			commonNameTagType = CommonNameTagType.threeLetterCode;

		for (int i = 0; i < headers.length; i++) {
			String header = headers[i];

			if (scientificColumnName != null && scientificColumnName.equalsIgnoreCase(header))
				scientificColumnIndex = i;
			else if (synonymColumnName != null && synonymColumnName.equalsIgnoreCase(header))
				synonymColumnIndex = i;
			else if (rankColumnName != null && rankColumnName.equalsIgnoreCase(header))
				rankColumnIndex = i;
			else if (statusColumnName != null && statusColumnName.equalsIgnoreCase(header))
				statusColumnIndex = i;
			else if (positionColumnName != null && positionColumnName.equalsIgnoreCase(header))
				positionColumnIndex = i;
			else if (sourceColumnName != null && sourceColumnName.equalsIgnoreCase(header))
				sourceColumnIndex = i;
			else if (sourceIdColumnName != null && sourceIdColumnName.equalsIgnoreCase(header))
				sourceIdColumnIndex = i;
			else if (commonNameColumn != null && commonNameColumn.equalsIgnoreCase(header))
				commonNameColumnIndex = i;
			else if (nameToRank.containsKey(header)) {
				String rank = nameToRank.get(header);
				rankToIndex.put(rank, i);
			}
		}
	}

	public TaxonomySave readOneRow(UtilityServiceApi utilityServiceApi, String[] data) throws ApiException {
		TaxonomySave taxonomySave = new TaxonomySave();
		if (scientificColumnIndex == -1 || rankColumnIndex == -1 || data[scientificColumnIndex] == null
				|| "".equals(data[scientificColumnIndex]) || data[rankColumnIndex] == null
				|| "".equals(data[rankColumnIndex])) {
			return null;
		} else {
			taxonomySave.setScientificName(data[scientificColumnIndex]);
			taxonomySave.setRank(data[rankColumnIndex]);
		}

		if (statusColumnIndex == -1 || data[statusColumnIndex] == null || "".equals(data[statusColumnIndex]))
			taxonomySave.setStatus(TaxonomyStatus.ACCEPTED);
		else
			taxonomySave.setStatus(TaxonomyStatus.fromValue(data[statusColumnIndex].toUpperCase()));

		if (positionColumnIndex == -1 || data[positionColumnIndex] == null || "".equals(data[positionColumnIndex]))
			taxonomySave.setPosition(TaxonomyPosition.RAW);
		else
			taxonomySave.setPosition(TaxonomyPosition.fromValue(data[positionColumnIndex].toUpperCase()));

		if (synonymColumnIndex != -1)
			taxonomySave.setSynonyms(data[synonymColumnIndex]);
		if (sourceColumnIndex != -1)
			taxonomySave.setSource(data[sourceColumnIndex]);
		if (sourceIdColumnIndex != -1)
			taxonomySave.setSourceId(data[sourceIdColumnIndex]);

		Map<String, String> rankToName = new HashMap<>();
		for (Map.Entry<String, Integer> entry : rankToIndex.entrySet()) {
			String rank = entry.getKey();
			Integer index = entry.getValue();
			if (data[index] == null || "".equals(data[index].trim()))
				continue;
			rankToName.put(rank, data[index]);
		}
		taxonomySave.setRankToName(rankToName);

		if (commonNameColumnIndex != -1 && data[commonNameColumnIndex] != null && !"".equals(data[commonNameColumnIndex])) {
			String commonNameColumnValue = data[commonNameColumnIndex];
			List<String> otherCommonNames = new ArrayList<>(); 
			Map<Long, String[]> commonNames = new HashMap<>();
			String[] commonNameForLanguage = commonNameColumnValue.split(";");
			for (String cName : commonNameForLanguage) {
				String[] languageCName = cName.split(":");
				String languageType = languageCName[0].trim();
				String commonNameString = languageCName[1].trim();
				if (languageType != null && commonNameString != null) {
					try {
					Language language = utilityServiceApi.getLanguage(languageType, commonNameTagType.name());
					Long languageId = language.getId();
					commonNames.put(languageId, commonNameString.split(","));
					} catch (ApiException | NoResultException e) {
						otherCommonNames.addAll(Arrays.asList(commonNameString.split(",")));
					}
				}
			}
			if(!otherCommonNames.isEmpty())
				commonNames.put(null, otherCommonNames.toArray(new String[0]));
			taxonomySave.setCommonNames(commonNames);
		}
		return taxonomySave;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public Map<String, String> getNameToRank() {
		return nameToRank;
	}

	public void setNameToRank(Map<String, String> nameToRank) {
		this.nameToRank = nameToRank;
	}

	public String getScientificColumnName() {
		return scientificColumnName;
	}

	public void setScientificColumnName(String scientificColumnName) {
		this.scientificColumnName = scientificColumnName;
	}

	public String getSynonymColumnName() {
		return synonymColumnName;
	}

	public void setSynonymColumnName(String synonymColumnName) {
		this.synonymColumnName = synonymColumnName;
	}

	public String getRankColumnName() {
		return rankColumnName;
	}

	public void setRankColumnName(String rankColumnName) {
		this.rankColumnName = rankColumnName;
	}

	public String getStatusColumnName() {
		return statusColumnName;
	}

	public void setStatusColumnName(String statusColumnName) {
		this.statusColumnName = statusColumnName;
	}

	public String getPositionColumnName() {
		return positionColumnName;
	}

	public void setPositionColumnName(String positionColumnName) {
		this.positionColumnName = positionColumnName;
	}

	public String getSourceColumnName() {
		return sourceColumnName;
	}

	public void setSourceColumnName(String sourceColumnName) {
		this.sourceColumnName = sourceColumnName;
	}

	public String getSourceIdColumnName() {
		return sourceIdColumnName;
	}

	public void setSourceIdColumnName(String sourceIdColumnName) {
		this.sourceIdColumnName = sourceIdColumnName;
	}

	public CommonNameTagType getCommonNameTagType() {
		return commonNameTagType;
	}

	public void setCommonNameTagType(CommonNameTagType commonNameTagType) {
		this.commonNameTagType = commonNameTagType;
	}

	public String getCommonNameColumn() {
		return commonNameColumn;
	}

	public void setCommonNameColumn(String commonNameColumn) {
		this.commonNameColumn = commonNameColumn;
	}

	public int getScientificColumnIndex() {
		return scientificColumnIndex;
	}

	public void setScientificColumnIndex(int scientificColumnIndex) {
		this.scientificColumnIndex = scientificColumnIndex;
	}

	public int getSynonymColumnIndex() {
		return synonymColumnIndex;
	}

	public void setSynonymColumnIndex(int synonymColumnIndex) {
		this.synonymColumnIndex = synonymColumnIndex;
	}

	public int getRankColumnIndex() {
		return rankColumnIndex;
	}

	public void setRankColumnIndex(int rankColumnIndex) {
		this.rankColumnIndex = rankColumnIndex;
	}

	public int getStatusColumnIndex() {
		return statusColumnIndex;
	}

	public void setStatusColumnIndex(int statusColumnIndex) {
		this.statusColumnIndex = statusColumnIndex;
	}

	public int getPositionColumnIndex() {
		return positionColumnIndex;
	}

	public void setPositionColumnIndex(int positionColumnIndex) {
		this.positionColumnIndex = positionColumnIndex;
	}

	public int getSourceColumnIndex() {
		return sourceColumnIndex;
	}

	public void setSourceColumnIndex(int sourceColumnIndex) {
		this.sourceColumnIndex = sourceColumnIndex;
	}

	public int getSourceIdColumnIndex() {
		return sourceIdColumnIndex;
	}

	public void setSourceIdColumnIndex(int sourceIdColumnIndex) {
		this.sourceIdColumnIndex = sourceIdColumnIndex;
	}

	public int getCommonNameColumnIndex() {
		return commonNameColumnIndex;
	}

	public void setCommonNameColumnIndex(int commonNameColumnIndex) {
		this.commonNameColumnIndex = commonNameColumnIndex;
	}

	public Map<String, Integer> getRankToIndex() {
		return rankToIndex;
	}

	public void setRankToIndex(Map<String, Integer> rankToIndex) {
		this.rankToIndex = rankToIndex;
	}

}
