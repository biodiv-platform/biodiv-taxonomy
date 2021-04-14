package com.strandls.taxonomy.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.pac4j.core.profile.CommonProfile;

import com.strandls.authentication_utility.util.AuthUtil;
import com.strandls.taxonomy.pojo.Rank;
import com.strandls.taxonomy.service.exception.UnRecongnizedRankException;
import com.strandls.utility.pojo.ParsedName;

import net.minidev.json.JSONArray;

public class TaxonomyUtil {

	public static final String SPECIES = "species";
	public static final String INFRA_SPECIES = "infraspecies";

	public static final String UNINOMIAL = "uninomial";
	public static final String GENUS = "genus";
	public static final String SUB_GENUS = "subgenus";
	public static final String SPECIFIC_EPITHET = "specificEpithet";
	public static final String INFRA_SPECIFIC_EPITHETS = "infraspecificEpithets";
	public static final String INFRA_SPECIFIC_EPITHET = "infraspecificEpithet";

	private TaxonomyUtil() {

	}

	public static Double getHighestInputRank(List<Rank> ranks, Map<String, String> inputRanks) {
		Double highestRank = -1.0;
		for (Rank rank : ranks)
			if (inputRanks.containsKey(rank.getName()))
				highestRank = highestRank < rank.getRankValue() ? rank.getRankValue() : highestRank;

		return highestRank;
	}

	@SuppressWarnings("unchecked")
	public static String getRankForSynonym(ParsedName parsedName, String acceptedRank)
			throws UnRecongnizedRankException {
		if (parsedName == null)
			return acceptedRank;

		List<Object> details = parsedName.getDetails();
		if (details == null || details.isEmpty())
			return acceptedRank;

		if (details.get(0) instanceof LinkedHashMap) {
			Map<String, Object> m = (Map<String, Object>) details.get(0);
			if (m.containsKey(INFRA_SPECIFIC_EPITHETS))
				return INFRA_SPECIES;
			else if (m.containsKey(SPECIFIC_EPITHET))
				return SPECIES;
			else if (m.containsKey(UNINOMIAL)
					&& (INFRA_SPECIES.equalsIgnoreCase(acceptedRank) || SPECIES.equalsIgnoreCase(acceptedRank))) {
				throw new UnRecongnizedRankException("Getting uninomial rank for " + parsedName.getVerbatim());
			}
		}

		return acceptedRank;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String getItalicisedForm(ParsedName sciName, String rankName) {

		String name = sciName.getVerbatim();

		if (sciName.getPositions() == null || sciName.getPositions().isEmpty())
			return name;

		if (!rankName.equalsIgnoreCase(GENUS) && !rankName.equalsIgnoreCase(SUB_GENUS)
				&& !rankName.equalsIgnoreCase(SPECIES) && !rankName.equalsIgnoreCase(INFRA_SPECIES))
			return name;

		StringBuilder italicisedFormBuilder = new StringBuilder();
		int index = 0;
		for (Object positions : sciName.getPositions()) {
			ArrayList<Object> position = (ArrayList) positions;

			int start = (int) position.get(1);
			int end = (int) position.get(2);

			if (start > index)
				italicisedFormBuilder.append(name.substring(index, start));

			if (position.get(0).equals(GENUS) || position.get(0).equals(SPECIFIC_EPITHET)
					|| position.get(0).equals(INFRA_SPECIFIC_EPITHET) || position.get(0).equals(UNINOMIAL))
				italicisedFormBuilder.append("<i>" + name.substring(start, end) + "</i>");
			else
				italicisedFormBuilder.append(name.substring(start, end));
			index = end;
		}
		if (index < name.length())
			italicisedFormBuilder.append(name.substring(index));

		String italicisedForm = italicisedFormBuilder.toString();
		italicisedForm = italicisedForm.replaceAll("</i>\\s*<i>", " ");
		italicisedForm = italicisedForm.replaceAll("</i>\\s*,\\s*<i>", ", ");
		italicisedForm = italicisedForm.replaceAll("<i>\\s*</i>", " ");
		italicisedForm = italicisedForm.replaceAll("<i>\\s*,\\s*</i>", ", ");
		italicisedForm = italicisedForm.trim();

		return italicisedForm;
	}

	public static boolean isAdmin(HttpServletRequest request) {
		if (request == null)
			return false;

		CommonProfile profile = AuthUtil.getProfileFromRequest(request);
		if (profile == null)
			return false;

		JSONArray roles = (JSONArray) profile.getAttribute("roles");
		return roles.contains("ROLE_ADMIN");
	}
}
