package com.strandls.taxonomy.service.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;

import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strandls.authentication_utility.util.AuthUtil;
import com.strandls.taxonomy.ApiConstants;
import com.strandls.taxonomy.dao.CommonNameDao;
import com.strandls.taxonomy.pojo.CommonName;
import com.strandls.taxonomy.pojo.CommonNamesData;
import com.strandls.taxonomy.service.CommonNameSerivce;
import com.strandls.taxonomy.util.AbstractService;
import com.strandls.utility.controller.LanguageServiceApi;
import com.strandls.utility.pojo.Language;

public class CommonNameServiceImpl extends AbstractService<CommonName> implements CommonNameSerivce {

	@Inject
	private CommonNameDao commonNameDao;

	@Inject
	private LanguageServiceApi languageService;

	@Inject
	private LogActivities logActivity;
	
	@Inject
	private TaxonomyESOperation taxonomyESOperation;

	@Inject
	public CommonNameServiceImpl(CommonNameDao dao) {
		super(dao);
	}

	private final Logger logger = LoggerFactory.getLogger(CommonNameServiceImpl.class);

	@Override
	public CommonName fetchById(Long id) {
		return commonNameDao.findById(id);
	}

	@Override
	public List<CommonName> fetchByTaxonId(Long taxonId) {
		return commonNameDao.fetchByTaxonId(taxonId);
	}

	@Override
	public List<CommonName> getCommonName(Long languageId, Long taxonConceptId, String commonNameString) {
		return commonNameDao.getCommonName(languageId, taxonConceptId, commonNameString);
	}

	@Override
	public CommonName getPrefferedCommonName(Long taxonId) {
		List<CommonName> commonNames = fetchByTaxonId(taxonId);
		for (CommonName commonName : commonNames) {
			if (commonName.getIsPreffered())
				return commonName;
		}
		return null;
	}

	@Override
	public CommonName updateIsPreffered(Long id) {
		CommonName commonName = commonNameDao.findById(id);
		Long taxonConceptId = commonName.getTaxonConceptId();

		List<CommonName> commonNames = fetchByTaxonId(taxonConceptId);
		for (CommonName c : commonNames) {
			c.setIsPreffered(false);
			update(c);
		}
		commonName.setIsPreffered(true);
		return update(commonName);
	}

	public List<CommonName> addCommonNames(Long taxonConceptId, Map<Long, String[]> languageIdToCommonNames,
			String source) {

		List<CommonName> commonNamesList = new ArrayList<>();

		for (Map.Entry<Long, String[]> entry : languageIdToCommonNames.entrySet()) {
			Long languageId = entry.getKey();
			String[] commonNameStrings = entry.getValue();
			for (String commonNameString : commonNameStrings) {
				List<CommonName> commonNames = getCommonName(languageId, taxonConceptId, commonNameString);
				if (commonNames.isEmpty()) {
					CommonName commonName = createCommonName(languageId, commonNameString, taxonConceptId, source);
					commonNamesList.add(commonName);
				}
			}
		}

		return commonNamesList;
	}

	private CommonName createCommonName(Long languageId, String commonNameString, Long taxonConceptId, String source) {
		Timestamp uploadTime = new Timestamp(new Date().getTime());
		Long uploaderId = TaxonomyDefinitionServiceImpl.UPLOADER_ID;
		String transliteration = "";

		CommonName commonName = new CommonName();
		commonName.setLanguageId(languageId);
		commonName.setName(commonNameString.trim());
		commonName.setTaxonConceptId(taxonConceptId);
		commonName.setUploadTime(uploadTime);
		commonName.setUploaderId(uploaderId);
		commonName.setTransliteration(transliteration);
		commonName.setViaDatasource(source);
		commonName.setIsDeleted(false);

		commonName = save(commonName);
		return commonName;
	}

	@Override
	public List<CommonName> updateAddCommonName(HttpServletRequest request, Long speciesId,
			CommonNamesData commonNamesData) {

		try {
			CommonProfile profile = AuthUtil.getProfileFromRequest(request);
			Long uploaderId = Long.parseLong(profile.getId());
			if (commonNamesData.getName() == null || commonNamesData.getTaxonConceptId() == null)
				return new ArrayList<>();
			if (commonNamesData.getId() == null) {
				CommonName commonNames = new CommonName(null, commonNamesData.getLanguageId(),
						commonNamesData.getName(), commonNamesData.getTaxonConceptId(), new Date(), uploaderId, null,
						false, null, false);

				commonNames = commonNameDao.save(commonNames);
				String desc = "Added common name : " + commonNames.getName();
				logActivity.logActivity(request.getHeader(HttpHeaders.AUTHORIZATION), desc, speciesId, speciesId,
						ApiConstants.SPECIES, commonNames.getId(), "Added common name", null);

			} else {
				CommonName commonName = commonNameDao.findById(commonNamesData.getId());
				if (!commonName.getTaxonConceptId().equals(commonNamesData.getTaxonConceptId()))
					return new ArrayList<>();
				commonName.setName(commonNamesData.getName());
				if (commonNamesData.getLanguageId() != null)
					commonName.setLanguageId(commonNamesData.getLanguageId());

				commonName = commonNameDao.update(commonName);
				String desc = "Updated common name : " + commonName.getName();
				logActivity.logActivity(request.getHeader(HttpHeaders.AUTHORIZATION), desc, speciesId, speciesId,
						"species", commonName.getId(), "Updated common name", null);

			}
			Long taxonId = commonNamesData.getTaxonConceptId();
			List<CommonName> result = fetchCommonNameWithLangByTaxonId(taxonId);
			List<Long> taxonIds = new ArrayList<>();
			taxonIds.add(taxonId);
			taxonomyESOperation.pushToElastic(taxonIds);
			return result;

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return new ArrayList<>();

	}

	@Override
	public List<CommonName> removeCommonName(HttpServletRequest request, Long speciesId, Long commonNameId) {
		try {
			CommonName commonName = commonNameDao.findById(commonNameId);
			commonName = commonNameDao.delete(commonName);

			String desc = "Deleted common name : " + commonName.getName();
			logActivity.logActivity(request.getHeader(HttpHeaders.AUTHORIZATION), desc, speciesId, speciesId, "species",
					commonName.getId(), "Deleted common name", null);
			List<CommonName> result = fetchCommonNameWithLangByTaxonId(commonName.getTaxonConceptId());
			
			// Push to elastic 
			List<Long> taxonIds = new ArrayList<>();
			taxonIds.add(commonName.getTaxonConceptId());
			taxonomyESOperation.pushToElastic(taxonIds);
			
			return result;
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return new ArrayList<>();
	}

	@Override
	public List<CommonName> fetchCommonNameWithLangByTaxonId(Long taxonId) {

		try {
			List<CommonName> result = commonNameDao.findByTaxonId(taxonId);
			for (CommonName commonNames : result) {
				if (commonNames.getLanguageId() != null) {
					Language language = languageService.fetchLanguageById(commonNames.getLanguageId().toString());
					commonNames.setLanguage(language);
				}
			}
			return result;
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return new ArrayList<>();
	}
}
