/**
 * 
 */
package com.strandls.taxonomy.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.strandls.authentication_utility.util.AuthUtil;
import com.strandls.taxonomy.dao.AcceptedSynonymDao;
import com.strandls.taxonomy.dao.TaxonomyDefinitionDao;
import com.strandls.taxonomy.dao.TaxonomyRegistryDao;
import com.strandls.taxonomy.pojo.AcceptedSynonym;
import com.strandls.taxonomy.pojo.CommonName;
import com.strandls.taxonomy.pojo.Rank;
import com.strandls.taxonomy.pojo.SynonymData;
import com.strandls.taxonomy.pojo.TaxonomicNames;
import com.strandls.taxonomy.pojo.TaxonomyDefinition;
import com.strandls.taxonomy.pojo.TaxonomyRegistry;
import com.strandls.taxonomy.pojo.enumtype.TaxonomyPosition;
import com.strandls.taxonomy.pojo.enumtype.TaxonomyStatus;
import com.strandls.taxonomy.pojo.request.FileMetadata;
import com.strandls.taxonomy.pojo.request.TaxonomyPositionUpdate;
import com.strandls.taxonomy.pojo.request.TaxonomySave;
import com.strandls.taxonomy.pojo.request.TaxonomyStatusUpdate;
import com.strandls.taxonomy.pojo.response.TaxonomyDefinitionAndRegistry;
import com.strandls.taxonomy.pojo.response.TaxonomyNameListResponse;
import com.strandls.taxonomy.pojo.response.TaxonomyRegistryResponse;
import com.strandls.taxonomy.pojo.response.TaxonomySearch;
import com.strandls.taxonomy.service.CommonNameSerivce;
import com.strandls.taxonomy.service.RankSerivce;
import com.strandls.taxonomy.service.TaxonomyDefinitionSerivce;
import com.strandls.taxonomy.service.exception.TaxonCreationException;
import com.strandls.taxonomy.service.exception.UnRecongnizedRankException;
import com.strandls.taxonomy.util.AbstractService;
import com.strandls.taxonomy.util.TaxonomyCache;
import com.strandls.taxonomy.util.TaxonomyUtil;
import com.strandls.utility.ApiException;
import com.strandls.utility.controller.UtilityServiceApi;
import com.strandls.utility.pojo.ParsedName;

/**
 * 
 * @author Vilay
 *
 */
public class TaxonomyDefinitionServiceImpl extends AbstractService<TaxonomyDefinition>
		implements TaxonomyDefinitionSerivce {

	@Inject
	private TaxonomyDefinitionDao taxonomyDao;

	@Inject
	private TaxonomyESOperation taxonomyESUpdate;

	@Inject
	private TaxonomyRegistryDao taxonomyRegistryDao;

	@Inject
	private UtilityServiceApi utilityServiceApi;

	@Inject
	private RankSerivce rankService;

	@Inject
	private AcceptedSynonymDao acceptedSynonymDao;

	@Inject
	private CommonNameSerivce commonNameSerivce;

	@Inject
	private ObjectMapper objectMapper;

	@Inject
	private TaxonomyCache taxonomyCache;

	@Inject
	private LogActivities logActivity;

	private final Logger logger = LoggerFactory.getLogger(TaxonomyDefinitionServiceImpl.class);

	static final Long UPLOADER_ID = 1L;

	@Inject
	public TaxonomyDefinitionServiceImpl(TaxonomyDefinitionDao dao) {
		super(dao);
	}

	@Override
	public TaxonomyDefinition fetchById(Long id) {
		return taxonomyDao.findById(id);
	}

	@Override
	public List<TaxonomyDefinition> saveList(HttpServletRequest request, List<TaxonomySave> taxonomyList)
			throws ApiException {
		List<TaxonomyDefinition> taxonomyDefinitions = new ArrayList<>();
		for (TaxonomySave taxonomySave : taxonomyList) {
			TaxonomyDefinition taxonomyDefinition = save(request, taxonomySave);
			taxonomyDefinitions.add(taxonomyDefinition);
		}
		return taxonomyDefinitions;
	}

	@Override
	public TaxonomyDefinition save(HttpServletRequest request, TaxonomySave taxonomySave) throws ApiException {

		List<TaxonomyDefinition> taxonomyDefinitions = addTaxonomyDefintionNodes(request, taxonomySave);

		TaxonomyDefinition taxonomyDefinition = null;
		if (taxonomyDefinitions.isEmpty())
			return taxonomyDefinition;

		List<Long> taxonIds = new ArrayList<>();
		for (TaxonomyDefinition td : taxonomyDefinitions) {
			taxonIds.add(td.getId());

			if (td.getRank().equalsIgnoreCase(taxonomySave.getRank())
					&& td.getStatus().equalsIgnoreCase(taxonomySave.getStatus().name()))
				taxonomyDefinition = td;
		}

		taxonomyESUpdate.pushToElastic(taxonIds);

		return taxonomyDefinition;
	}

	private List<TaxonomyDefinition> addTaxonomyDefintionNodes(HttpServletRequest request, TaxonomySave taxonomySave)
			throws ApiException {
		List<TaxonomyDefinition> createdTaxonomy;
		try {
			createdTaxonomy = addTaxonomy(request, taxonomySave);
		} catch (TaxonCreationException | UnRecongnizedRankException e) {
			return new ArrayList<>();
		}

		if (createdTaxonomy.isEmpty())
			return new ArrayList<>();

		return createdTaxonomy;
	}

	public TaxonomyDefinition createNotAssignedName(String rank, TaxonomyPosition position, Long userId)
			throws UnRecongnizedRankException, ApiException, TaxonCreationException {
		ParsedName parsedName = taxonomyCache.getName(rank, "Not assigned");
		return taxonomyDao.createTaxonomyDefiniiton(parsedName, rank, TaxonomyStatus.ACCEPTED, position, "", "",
				userId);
	}

	private TaxonomyDefinition createRoot(StringBuilder path) throws ApiException, TaxonCreationException {
		ParsedName parsedName = utilityServiceApi.getNameParsed("Root");
		TaxonomyDefinition taxonomyDefinition = taxonomyDao.createTaxonomyDefiniiton(parsedName, "root",
				TaxonomyStatus.ACCEPTED, TaxonomyPosition.CLEAN, null, null, null);
		Long taxonomyDefinitionId = taxonomyDefinition.getId();

		path.append(taxonomyDefinitionId);
		taxonomyRegistryDao.createRegistry(null, path.toString(), "root", taxonomyDefinitionId, null, null);

		return taxonomyDefinition;
	}

	private TaxonomyDefinition updateTaxonomyDefinition(Long taxonId, ParsedName parsedName, String rankName,
			TaxonomyStatus taxonomyStatus, TaxonomyPosition taxonomyPosition, String source, String sourceId,
			Long uploaderId) {

		TaxonomyDefinition taxonomyDefinition = taxonomyDao.findById(taxonId);

		String canonicalName = parsedName.getCanonicalName().getFull();
		String binomialName = TaxonomyUtil.getBinomialName(canonicalName);
		String italicisedForm = TaxonomyUtil.getItalicisedForm(parsedName, rankName);
		String status = taxonomyStatus.name();
		String position = taxonomyPosition.name();

		taxonomyDefinition.setBinomialForm(binomialName);
		taxonomyDefinition.setCanonicalForm(canonicalName);
		taxonomyDefinition.setItalicisedForm(italicisedForm);
		taxonomyDefinition.setName(parsedName.getVerbatim().trim());
		taxonomyDefinition.setNormalizedForm(parsedName.getNormalized());
		taxonomyDefinition.setRank(rankName);
		taxonomyDefinition.setStatus(status);
		taxonomyDefinition.setPosition(position);
		taxonomyDefinition.setUploaderId(uploaderId);
		taxonomyDefinition.setViaDatasource(source);
		taxonomyDefinition.setNameSourceId(sourceId);
		taxonomyDefinition.setAuthorYear(parsedName.getAuthorship());

		taxonomyDefinition = taxonomyDao.update(taxonomyDefinition);
		return taxonomyDefinition;

	}

	private List<TaxonomyDefinition> addTaxonomy(HttpServletRequest request, TaxonomySave taxonomyData)
			throws ApiException, TaxonCreationException, UnRecongnizedRankException {

		CommonProfile profile = AuthUtil.getProfileFromRequest(request);
		Long userId = Long.parseLong(profile.getId());

		// TODO : check authorization here

		// Check for the valid hierarchy if the status is accepted.
		TaxonomyStatus status = taxonomyData.getStatus();
		TaxonomyPosition position = taxonomyData.getPosition();
		List<Rank> ranks = taxonomyCache.getRanks();

		String source = taxonomyData.getSource();
		String sourceId = taxonomyData.getSourceId();

		String rankName = taxonomyData.getRank().toLowerCase();
		String scientificName = taxonomyData.getScientificName();

		StringBuilder path = new StringBuilder();

		ParsedName parsedName = taxonomyCache.getName(rankName, scientificName);
		TaxonomyDefinition taxonomyDefinition = getLeafMatchedNode(parsedName, rankName, status);

		List<TaxonomyDefinition> taxonomyDefinitions = new ArrayList<>();

		// Create the node if doesn't exist
		if (taxonomyDefinition == null) {

			// Create and update hierarchy if not exist
			if (status.equals(TaxonomyStatus.ACCEPTED)) {
				Map<String, TaxonomyDefinition> hierarchyCreated = updateAndCreateHierarchyUtil(path, ranks, userId,
						taxonomyData);
				taxonomyDefinitions.addAll(hierarchyCreated.values());
			}

			// Name can be synonym as well, so kept the hierarchy creation and registry
			// creation separate
			taxonomyDefinition = taxonomyDao.createTaxonomyDefiniiton(parsedName, rankName, status, position, source,
					sourceId, userId);
			taxonomyDefinitions.add(taxonomyDefinition);

			// If the status is accepted add node to registry
			if (status.equals(TaxonomyStatus.ACCEPTED)) {
				Long taxonId = taxonomyDefinition.getId();
				path.append(".");
				path.append(taxonId);
				taxonomyRegistryDao.createRegistry(null, path.toString(), rankName, taxonId, userId, null);
			}
		}

		// If it is synonym add it to the accepted synonym table
		if (status.equals(TaxonomyStatus.SYNONYM)) {
			Long synonymId = taxonomyDefinition.getId();
			Long acceptedId = taxonomyData.getAcceptedId();
			if (acceptedId == null)
				throw new TaxonCreationException("Accepted Id is required for the synonym");
			acceptedSynonymDao.createAcceptedSynonym(acceptedId, synonymId);
		}

		// Add Synonyms
		String synonymString = taxonomyData.getSynonyms();
		if (synonymString != null && !"".equals(synonymString.trim())) {

			Long acceptedId = taxonomyDefinition.getId();

			SynonymData synonymData = new SynonymData();
			synonymData.setName(synonymString.trim());
			synonymData.setRank(rankName);
			synonymData.setDataSource(source);
			synonymData.setDataSourceId(sourceId);

			List<TaxonomyDefinition> synonyms = addSynonym(request, acceptedId, synonymData);
			taxonomyDefinitions.addAll(synonyms);
		}

		// Add all common name
		if (taxonomyData.getCommonNames() != null) {
			Long taxonId = taxonomyDefinition.getId();
			Map<Long, String[]> languageIdToCommonNames = taxonomyData.getCommonNames();
			if (languageIdToCommonNames != null && !languageIdToCommonNames.isEmpty()) {
				commonNameSerivce.addCommonNames(taxonId, languageIdToCommonNames, source);
			}
		}

		return taxonomyDefinitions;
	}

	/**
	 * This is internally used by the addTaxonomyMethod
	 * 
	 * @param request
	 * @param taxonId
	 * @param synonymData
	 * @return
	 * @throws ApiException
	 * @throws UnRecongnizedRankException
	 */
	private List<TaxonomyDefinition> addSynonym(HttpServletRequest request, Long taxonId, SynonymData synonymData)
			throws ApiException, UnRecongnizedRankException {
		List<TaxonomyDefinition> taxonomyDefinitions = new ArrayList<>();

		String name = synonymData.getName();
		String source = synonymData.getDataSource();
		String sourceId = synonymData.getDataSourceId();
		String rankName = synonymData.getRank();

		for (String synonymName : name.split(";")) {
			synonymName = synonymName.trim();

			ParsedName synonymParsedName;
			String synonymRank;

			// If any of these exception occurs then we are skipping the synonym.
			try {
				synonymParsedName = taxonomyCache.getName(rankName, synonymName);
				synonymRank = TaxonomyUtil.getRankForSynonym(synonymParsedName, rankName);
			} catch (ApiException e) {
				logger.error("Invalie name for the synonym : " + synonymName);
				continue;
			} catch (UnRecongnizedRankException e) {
				logger.error("Unrecognized rank for the synonym : " + synonymName);
				continue;
			}

			TaxonomySave taxonomyData = new TaxonomySave();
			taxonomyData.setPosition(TaxonomyPosition.RAW);
			taxonomyData.setRank(synonymRank);
			taxonomyData.setScientificName(synonymName);
			taxonomyData.setSource(source);
			taxonomyData.setSourceId(sourceId);
			taxonomyData.setStatus(TaxonomyStatus.SYNONYM);
			taxonomyData.setAcceptedId(taxonId);

			List<TaxonomyDefinition> synonyms;
			try {
				synonyms = addTaxonomy(request, taxonomyData);
			} catch (TaxonCreationException e) {
				logger.error("Synonym Creation failed : " + synonymName);
				continue;
			}

			taxonomyDefinitions.addAll(synonyms);
		}
		return taxonomyDefinitions;

	}

	/**
	 * Utility to update and create the hierarchy
	 * 
	 * @param path         - String builder which store the path for hierarchy.
	 *                     (call by reference variable)
	 * @param ranks        - Pull of object for the available rank in the system.
	 * @param userId       - userId
	 * @param taxonomyData - taxonomy Data to push in the DB
	 * @return
	 * @throws ApiException               - If there is any micro-service down.
	 * @throws TaxonCreationException     - There is problem with hierarchy
	 *                                    creation.
	 * @throws UnRecongnizedRankException
	 */
	private Map<String, TaxonomyDefinition> updateAndCreateHierarchyUtil(StringBuilder path, List<Rank> ranks,
			Long userId, TaxonomySave taxonomyData)
			throws ApiException, TaxonCreationException, UnRecongnizedRankException {

		Map<String, String> rankToName = taxonomyData.getRankToName();
		String source = taxonomyData.getSource();
		String sourceId = taxonomyData.getSourceId();
		TaxonomyPosition position = taxonomyData.getPosition();

		rankToName.put("root", "Root");

		// Validate the rank with the leaf node as well.
		String rankName = taxonomyData.getRank().toLowerCase();
		rankToName.put(rankName, taxonomyData.getScientificName());

		if (!TaxonomyUtil.validateHierarchy(ranks, rankToName.keySet()))
			throw new TaxonCreationException("Invalid hierarchy");

		// Remove the leaf node For hierarchy update or creation.
		rankToName.remove(rankName);

		// Generate the rank to parse name pool of object.
		Map<String, ParsedName> rankToParsedName = new HashMap<>();
		for (Map.Entry<String, String> e : rankToName.entrySet()) {
			ParsedName parsedName = taxonomyCache.getName(e.getKey(), e.getValue());
			rankToParsedName.put(e.getKey().toLowerCase(), parsedName);
		}

		Map<String, TaxonomyDefinition> hierarchyCreated = updateAndCreateHierarchy(path, ranks, rankToParsedName,
				position, source, sourceId, userId);

		return hierarchyCreated;
	}

	private Map<String, TaxonomyDefinition> updateAndCreateHierarchy(StringBuilder path, List<Rank> ranks,
			Map<String, ParsedName> rankToParsedName, TaxonomyPosition position, String source, String sourceId,
			Long userId) throws ApiException, TaxonCreationException {

		String highestRankName = TaxonomyUtil.getHighestInputRankName(ranks, rankToParsedName.keySet());
		ParsedName parsedName = rankToParsedName.get(highestRankName);

		TaxonomyDefinition taxonomyDefinition = getHierarchyMatchedNode(parsedName, highestRankName, rankToParsedName);

		Map<String, TaxonomyDefinition> createdHierarchy;
		// Found the match for the taxonomy
		if (taxonomyDefinition != null) {
			String taxonPath = taxonomyRegistryDao.findbyTaxonomyId(taxonomyDefinition.getId(), null).getPath();
			path.append(taxonPath);
			return new LinkedHashMap<>();
		} else {
			// Not found the match then go up the hierarchy to find the match
			rankToParsedName.remove(highestRankName);
			if (highestRankName.equalsIgnoreCase("root")) {
				TaxonomyDefinition root = createRoot(path);
				createdHierarchy = new LinkedHashMap<>();
				createdHierarchy.put("root", root);
				return createdHierarchy;
			}

			createdHierarchy = updateAndCreateHierarchy(path, ranks, rankToParsedName, position, source, sourceId,
					userId);

			// Generate node here and go for the higher hierarchy
			taxonomyDefinition = taxonomyDao.createTaxonomyDefiniiton(parsedName, highestRankName,
					TaxonomyStatus.ACCEPTED, position, source, sourceId, userId);
			createdHierarchy.put(highestRankName, taxonomyDefinition);

			// Add the created taxonomy to the registry.
			Long taxonId = taxonomyDefinition.getId();
			path.append(".");
			path.append(taxonId);
			taxonomyRegistryDao.createRegistry(null, path.toString(), highestRankName, taxonId, userId, null);
		}

		return createdHierarchy;
	}

	private TaxonomyDefinition getHierarchyMatchedNode(ParsedName parsedName, String rankName,
			Map<String, ParsedName> rankToParsedName) throws ApiException {

		List<TaxonomyDefinition> taxonomyDefinitions = taxonomyDao
				.findByCanonicalForm(parsedName.getCanonicalName().getFull(), rankName);

		int maxScore = 0;
		TaxonomyDefinition matchedTaxonomy = null;
		for (TaxonomyDefinition taxonomyDefinition : taxonomyDefinitions) {

			List<TaxonomyRegistryResponse> hierarchy = taxonomyRegistryDao.getPathToRoot(taxonomyDefinition.getId(),
					null);
			int score = 0;

			for (TaxonomyRegistryResponse r : hierarchy) {
				String rank = r.getRank();
				String name = r.getCanonicalForm();
				ParsedName inputParsedName = rankToParsedName.get(rank);
				if (inputParsedName.getCanonicalName().getFull().equalsIgnoreCase(name))
					score++;
			}

			if (maxScore < score) {
				maxScore = score;
				matchedTaxonomy = taxonomyDefinition;
			}
		}
		return matchedTaxonomy;
	}

	private TaxonomyDefinition getLeafMatchedNode(ParsedName parsedName, String rankName, TaxonomyStatus status) {

		if (parsedName == null || parsedName.getCanonicalName() == null)
			return null;

		String canonicalName = parsedName.getCanonicalName().getFull();

		List<TaxonomyDefinition> taxonomyDefinitions = taxonomyDao.findByCanonicalForm(canonicalName, rankName);

		String verbatim = parsedName.getVerbatim().trim();
		for (TaxonomyDefinition taxonomyDefinition : taxonomyDefinitions) {
			if (!status.name().equalsIgnoreCase(taxonomyDefinition.getStatus()))
				continue;
			if (taxonomyDefinition.getName().equals(verbatim))
				return taxonomyDefinition;
		}
		return null;
	}

	@Override
	public Map<String, Object> uploadFile(HttpServletRequest request, FormDataMultiPart multiPart)
			throws IOException, ApiException, InterruptedException, ExecutionException {
		Map<String, Object> result = new HashMap<>();
		Long startTime = System.currentTimeMillis();
		FormDataBodyPart formdata = multiPart.getField("metadata");
		if (formdata == null) {
			throw new WebApplicationException(
					Response.status(Response.Status.BAD_REQUEST).entity("Metadata file not present").build());
		}
		InputStream metaDataInputStream = formdata.getValueAs(InputStream.class);
		InputStreamReader inputStreamReader = new InputStreamReader(metaDataInputStream, StandardCharsets.UTF_8);
		FileMetadata fileMetaData = objectMapper.readValue(inputStreamReader, FileMetadata.class);

		if ("csv".equalsIgnoreCase(fileMetaData.getFileType())) {
			formdata = multiPart.getField("csv");
			InputStream csvDataInputStream = formdata.getValueAs(InputStream.class);
			inputStreamReader = new InputStreamReader(csvDataInputStream, StandardCharsets.UTF_8);
			CSVReader reader = new CSVReader(inputStreamReader);
			Iterator<String[]> it = reader.iterator();
			String[] headers = it.next();
			fileMetaData.updateIndices(headers);

			List<Long> taxonIds = new ArrayList<>();
			while (it.hasNext()) {
				String[] data = it.next();
				TaxonomySave taxonomySave = fileMetaData.readOneRow(utilityServiceApi, data);
				if (taxonomySave != null) {

					List<TaxonomyDefinition> createdTaxonomy = addTaxonomyDefintionNodes(request, taxonomySave);

					// Push taxonomy to the elastic
					if (!createdTaxonomy.isEmpty()) {
						for (TaxonomyDefinition td : createdTaxonomy) {
							if (td != null)
								taxonIds.add(td.getId());
						}
					}
				}
			}
			if (!taxonIds.isEmpty()) {
				taxonomyESUpdate.pushToElastic(taxonIds);
			}
			reader.close();
		}
		Long endTime = System.currentTimeMillis();
		result.put("status", "success");
		result.put("uploadTime (in ms) : ", (endTime - startTime));
		result.put("Cache hit : ", TaxonomyCache.getCacheHit());
		result.put("Cache miss : ", TaxonomyCache.getCacheMiss());
		return result;
	}

	@Override
	public TaxonomicNames findSynonymCommonName(Long taxonId) {

		try {
			List<CommonName> commonNames = commonNameSerivce.fetchCommonNameWithLangByTaxonId(taxonId);
			List<TaxonomyDefinition> synonymList = findSynonyms(taxonId);

			TaxonomicNames result = new TaxonomicNames(commonNames, synonymList);
			return result;

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return null;

	}

	private List<TaxonomyDefinition> findSynonyms(Long taxonId) {
		List<AcceptedSynonym> acceptedSynonymsList = acceptedSynonymDao.findByAccepetdId(taxonId);
		List<TaxonomyDefinition> synonymList = new ArrayList<>();
		if (acceptedSynonymsList != null && !acceptedSynonymsList.isEmpty()) {
			for (AcceptedSynonym synonym : acceptedSynonymsList) {
				TaxonomyDefinition taxonomy = taxonomyDao.findById(synonym.getSynonymId());
				synonymList.add(taxonomy);
			}
		}
		return synonymList;
	}

	@Override
	public List<TaxonomyDefinition> updateAddSynonym(HttpServletRequest request, Long speciesId, Long taxonId,
			SynonymData synonymData) {

		try {
			ParsedName parsedName = utilityServiceApi.getNameParsed(synonymData.getName());
			String synonymRank = TaxonomyUtil.getRankForSynonym(parsedName, synonymData.getRank());
			CommonProfile profile = AuthUtil.getProfileFromRequest(request);

			Long userId = Long.parseLong(profile.getId());

			TaxonomyDefinition synonymTaxonomy = null;
			String desc = "";
			String activityType = "";
			Long synonymId = null;
			String name = "";
			TaxonomyDefinition synonymCheck = getLeafMatchedNode(parsedName, synonymRank, TaxonomyStatus.SYNONYM);

			if (synonymData.getId() == null) {
				if (synonymCheck == null) {

					synonymTaxonomy = taxonomyDao.createTaxonomyDefiniiton(parsedName, synonymRank,
							TaxonomyStatus.SYNONYM, TaxonomyPosition.RAW, synonymData.getDataSource(),
							synonymData.getDataSourceId(), userId);
					synonymId = synonymTaxonomy.getId();
					name = synonymTaxonomy.getName();
				} else {
					synonymId = synonymCheck.getId();
					name = synonymCheck.getName();
				}

				AcceptedSynonym acceptedSynonym = acceptedSynonymDao.findByAccpetedIdSynonymId(taxonId, synonymId);
				if (acceptedSynonym == null) {
					acceptedSynonym = new AcceptedSynonym();
					acceptedSynonym.setAcceptedId(taxonId);
					acceptedSynonym.setSynonymId(synonymId);
					acceptedSynonym.setVersion(0L);
					acceptedSynonymDao.save(acceptedSynonym);
				}

				desc = "Added synonym : " + name;

				activityType = "Added synonym";

			} else {

				if (synonymCheck == null) {
					synonymTaxonomy = updateTaxonomyDefinition(synonymData.getId(), parsedName, synonymRank,
							TaxonomyStatus.SYNONYM, TaxonomyPosition.RAW, synonymData.getDataSource(),
							synonymData.getDataSourceId(), userId);
					synonymId = synonymTaxonomy.getId();
					name = synonymTaxonomy.getName();
				} else {
//					delete previous synonym
					synonymId = synonymCheck.getId();
					name = synonymCheck.getName();
					AcceptedSynonym acceptedSynonym = acceptedSynonymDao.findByAccpetedIdSynonymId(taxonId,
							synonymData.getId());
					acceptedSynonymDao.delete(acceptedSynonym);
					TaxonomyDefinition taxnomyDefinition = taxonomyDao.findById(synonymData.getId());
					taxnomyDefinition.setIsDeleted(true);
					taxonomyDao.update(taxnomyDefinition);

//					map the exist synonym
					acceptedSynonym = new AcceptedSynonym();
					acceptedSynonym.setAcceptedId(taxonId);
					acceptedSynonym.setSynonymId(synonymId);
					acceptedSynonym.setVersion(0L);
					acceptedSynonymDao.save(acceptedSynonym);

				}

				desc = "Updated synonym : " + name;
				activityType = "Updated synonym";
			}

			logActivity.logActivity(request.getHeader(HttpHeaders.AUTHORIZATION), desc, speciesId, speciesId, "species",
					synonymId, activityType, null);

			return findSynonyms(taxonId);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return new ArrayList<>();
	}

	@Override
	public List<TaxonomyDefinition> deleteSynonym(HttpServletRequest request, Long speciesId, Long taxonId,
			Long synonymId) {
		try {
			AcceptedSynonym acceptedSynonym = acceptedSynonymDao.findByAccpetedIdSynonymId(taxonId, synonymId);
			acceptedSynonymDao.delete(acceptedSynonym);
			TaxonomyDefinition taxnomyDefinition = taxonomyDao.findById(synonymId);
			taxnomyDefinition.setIsDeleted(true);
			taxonomyDao.update(taxnomyDefinition);

			String desc = "Deleted synonym : " + taxnomyDefinition.getName();

			logActivity.logActivity(request.getHeader(HttpHeaders.AUTHORIZATION), desc, speciesId, speciesId, "species",
					taxnomyDefinition.getId(), "Deleted synonym", null);
			return findSynonyms(taxonId);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return new ArrayList<>();
	}

	@SuppressWarnings("unchecked")
	@Override
	public TaxonomySearch getByNameSearch(String scientificName, String rankName) throws ApiException {
		TaxonomySearch taxonomySearch = new TaxonomySearch();
		ParsedName parsedName = utilityServiceApi.getNameParsed(scientificName);
		String canonicalForm = parsedName.getCanonicalName().getFull();
		List<TaxonomyDefinition> taxonomyDefinitions = taxonomyDao.findByCanonicalForm(canonicalForm, rankName);
		if (taxonomyDefinitions.isEmpty()) {
			List<Object> details = parsedName.getDetails();
			if (details.get(0) instanceof LinkedHashMap) {
				Map<String, Object> m = (Map<String, Object>) details.get(0);
				if (m.containsKey(TaxonomyUtil.GENUS))
					canonicalForm = (String) ((LinkedHashMap<String, Object>) m.get(TaxonomyUtil.GENUS)).get("value");
				else if (m.containsKey(TaxonomyUtil.UNINOMIAL)) {
					canonicalForm = (String) ((LinkedHashMap<String, Object>) m.get(TaxonomyUtil.UNINOMIAL))
							.get("value");
				}
				taxonomyDefinitions = taxonomyDao.findByCanonicalForm(canonicalForm, TaxonomyUtil.GENUS);

				if (!taxonomyDefinitions.isEmpty()) {
					// Taking the first one to auto Fill
					List<TaxonomyDefinitionAndRegistry> parentMatched = new ArrayList<>();
					for (TaxonomyDefinition taxonomyDefinition : taxonomyDefinitions) {
						List<TaxonomyRegistryResponse> taxonomyRegistry = taxonomyRegistryDao
								.getPathToRoot(taxonomyDefinition.getId(), null);
						TaxonomyDefinitionAndRegistry taxonomyDefinitionAndRegistry = new TaxonomyDefinitionAndRegistry(
								taxonomyDefinition, taxonomyRegistry);
						parentMatched.add(taxonomyDefinitionAndRegistry);
					}
					taxonomySearch.setParentMatched(parentMatched);
				}
			}
		} else {
			List<TaxonomyDefinitionAndRegistry> matched = new ArrayList<>();
			for (TaxonomyDefinition taxonomyDefinition : taxonomyDefinitions) {
				List<TaxonomyRegistryResponse> taxonomyRegistry = taxonomyRegistryDao
						.getPathToRoot(taxonomyDefinition.getId(), null);
				TaxonomyDefinitionAndRegistry taxonomyDefinitionAndRegistry = new TaxonomyDefinitionAndRegistry(
						taxonomyDefinition, taxonomyRegistry);
				matched.add(taxonomyDefinitionAndRegistry);
			}
			taxonomySearch.setMatched(matched);
		}

		return taxonomySearch;
	}

	@Override
	public TaxonomyDefinition updateName(Long taxonId, String taxonName) throws ApiException {
		TaxonomyDefinition taxonomyDefinition;
		try {
			taxonomyDefinition = findById(taxonId);
		} catch (NoResultException e) {
			throw new NoResultException("Not able to find the given taxon");
		}

		ParsedName parsedName = utilityServiceApi.getNameParsed(taxonName);

		String name = parsedName.getVerbatim().trim();
		String normalizedName = parsedName.getNormalized();
		String canonicalName = parsedName.getCanonicalName().getFull();
		String binomialForm = TaxonomyUtil.getBinomialName(canonicalName);
		String italicisedForm = TaxonomyUtil.getItalicisedForm(parsedName, taxonomyDefinition.getRank());
		String authorShip = parsedName.getAuthorship();

		taxonomyDefinition.setName(name);
		taxonomyDefinition.setNormalizedForm(normalizedName);
		taxonomyDefinition.setCanonicalForm(canonicalName);
		taxonomyDefinition.setBinomialForm(binomialForm);
		taxonomyDefinition.setItalicisedForm(italicisedForm);
		taxonomyDefinition.setAuthorYear(authorShip);

		taxonomyDefinition = taxonomyDao.update(taxonomyDefinition);

		List<Long> taxonIds = taxonomyDao.getAllChildren(taxonId);

		taxonomyESUpdate.pushToElastic(taxonIds);

		logger.debug("Name update for the given taxonomy :" + taxonName);
		logger.debug("Hierarchy update for all the children : " + taxonIds);

		return taxonomyDefinition;
	}

	@Override
	public TaxonomyDefinition updateStatus(HttpServletRequest request, TaxonomyStatusUpdate taxonomyStatusUpdate)
			throws ApiException, TaxonCreationException {

		CommonProfile profile = AuthUtil.getProfileFromRequest(request);
		Long userId = Long.parseLong(profile.getId());

		Long taxonId = taxonomyStatusUpdate.getTaxonId();
		TaxonomyStatus taxonomyStatus = taxonomyStatusUpdate.getStatus();
		Map<String, String> hierarchy = taxonomyStatusUpdate.getHierarchy();
		Long newTaxonId = taxonomyStatusUpdate.getNewTaxonId();

		TaxonomyDefinition taxonomyDefinition;

		try {
			taxonomyDefinition = findById(taxonId);
		} catch (NoResultException e) {
			throw new NoResultException("Not able to find the given taxon");
		}

		if (taxonomyDefinition.getStatus().equalsIgnoreCase(taxonomyStatus.name())) {
			// Status is not changed so no need to update.
			return taxonomyDefinition;
		}

		switch (taxonomyStatus) {
		// Status is changing from synonym to accepted.
		case ACCEPTED:
			if (hierarchy == null)
				throw new IllegalArgumentException("Hierarchy is required");

			// Remove the link with any other accepted name
			List<AcceptedSynonym> acceptedSynonyms = acceptedSynonymDao.findBySynonymId(taxonId);
			for (AcceptedSynonym acceptedSynonym : acceptedSynonyms) {
				acceptedSynonymDao.delete(acceptedSynonym);
			}

			// Add the hierarchy and the node
			Map<String, ParsedName> rankToParsedName = new HashMap<>();
			for (Map.Entry<String, String> e : taxonomyStatusUpdate.getHierarchy().entrySet()) {
				ParsedName parsedName = utilityServiceApi.getNameParsed(e.getValue());
				rankToParsedName.put(e.getKey(), parsedName);
			}
			List<Rank> ranks = rankService.getAllRank(request);
			TaxonomyPosition position = TaxonomyPosition.fromValue(taxonomyDefinition.getPosition());

			StringBuilder path = new StringBuilder();
			updateAndCreateHierarchy(path, ranks, rankToParsedName, position, taxonomyDefinition.getViaDatasource(),
					taxonomyDefinition.getNameSourceId(), userId);

			// Update the tree and add to the registry
			path.append(".");
			path.append(taxonId);
			taxonomyRegistryDao.createRegistry(null, path.toString(), taxonomyDefinition.getRank(), taxonId, userId,
					null);

			// Update the status
			taxonomyDefinition.setStatus(TaxonomyStatus.ACCEPTED.name());
			taxonomyDefinition = update(taxonomyDefinition);

			// Update the elastic for all the accepted name it was associated and the node
			List<Long> taxonIds = new ArrayList<>();
			taxonIds.add(taxonId);
			taxonomyESUpdate.pushToElastic(taxonIds);

			break;
		// status is changing from accepted to synonym
		case SYNONYM:
			if (newTaxonId == null)
				throw new IllegalArgumentException("New taxonomy is required to assign all the children");

			TaxonomyDefinition acceptedTaxonomy = taxonomyDao.findById(newTaxonId);

			if (acceptedTaxonomy == null)
				throw new IllegalArgumentException("Could not find the accepted taxonomy with the Id you provided");

			taxonomyDefinition.setStatus(TaxonomyStatus.SYNONYM.name());

			// Taxonomy Id to be updated for elastic search
			taxonIds = taxonomyDao.getAllChildren(taxonId);
			acceptedSynonyms = acceptedSynonymDao.findByAccepetdId(taxonId);
			for (AcceptedSynonym acceptedSynonym : acceptedSynonyms) {
				taxonIds.add(acceptedSynonym.getSynonymId());
			}
			taxonIds.add(newTaxonId);

			// Make relevant update to the database.
			TaxonomyRegistry oldTaxonomyRegistry = taxonomyRegistryDao.findbyTaxonomyId(taxonId, null);
			TaxonomyRegistry newTaxonomyRegistry = taxonomyRegistryDao.findbyTaxonomyId(newTaxonId, null);
			taxonomyDao.updateStatusToSynonymInDB(newTaxonomyRegistry, oldTaxonomyRegistry);

			// Update the status for given taxon node.
			taxonomyDefinition = update(taxonomyDefinition);

			// Update elastic search for the taxon
			taxonomyESUpdate.pushToElastic(taxonIds);

			break;

		default:
			break;
		}

		return taxonomyDefinition;
	}

	@Override
	public TaxonomyDefinition updatePosition(HttpServletRequest request,
			TaxonomyPositionUpdate taxonomyPositionUpdate) {

		Long taxonId = taxonomyPositionUpdate.getTaxonId();
		TaxonomyPosition position = taxonomyPositionUpdate.getPosition();

		TaxonomyDefinition taxonomyDefinition = findById(taxonId);
		if (TaxonomyPosition.CLEAN.equals(position)
				|| TaxonomyPosition.CLEAN.name().equals(taxonomyDefinition.getPosition())) {
			// Not changing anything here just keeping as it is.
			return taxonomyDefinition;
		}
		
		if(!position.name().equals(taxonomyDefinition.getPosition())) {
			taxonomyDefinition.setPosition(position.name());
			update(taxonomyDefinition);
		}

		return taxonomyDefinition;
	}

	@Override
	public TaxonomyNameListResponse getTaxonomyNameList(Long taxonId, Long classificationId, String rankListString,
			String statusListString, String positionListString, Integer limit, Integer offset) throws IOException {

		// Verify the rank, add only lower case of it
		List<String> rankList = new ArrayList<>();
		if (rankListString == null || "".equals(rankListString))
			rankList = rankService.getAllRankNames();
		else {
			for (String rank : rankListString.split(","))
				rankList.add(rank.toLowerCase().trim());
		}

		// Verify and construct list of status
		List<String> statusList = TaxonomyStatus.getAllOrSpecified(statusListString);

		// verify and construct list of position
		List<String> positionList = TaxonomyPosition.getAllOrSpecified(positionListString);

		// Get the result based on query
		return taxonomyDao.getTaxonomyNameList(taxonId, classificationId, rankList, statusList, positionList, limit,
				offset);
	}
}
