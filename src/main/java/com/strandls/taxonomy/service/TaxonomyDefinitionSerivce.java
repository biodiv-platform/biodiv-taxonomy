/**
 * 
 */
package com.strandls.taxonomy.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import com.strandls.taxonomy.pojo.SynonymData;
import com.strandls.taxonomy.pojo.TaxonomicNames;
import com.strandls.taxonomy.pojo.TaxonomyDefinition;
import com.strandls.taxonomy.pojo.request.TaxonomyPositionUpdate;
import com.strandls.taxonomy.pojo.request.TaxonomySave;
import com.strandls.taxonomy.pojo.request.TaxonomyStatusUpdate;
import com.strandls.taxonomy.pojo.response.TaxonomyNameListResponse;
import com.strandls.taxonomy.pojo.response.TaxonomySearch;
import com.strandls.taxonomy.service.exception.TaxonCreationException;
import com.strandls.utility.ApiException;

/**
 * 
 * @author vilay
 *
 */
public interface TaxonomyDefinitionSerivce {

	public TaxonomyDefinition fetchById(Long id);

	public TaxonomyDefinition save(HttpServletRequest request, TaxonomySave taxonomySave) throws ApiException;

	public List<TaxonomyDefinition> saveList(HttpServletRequest request, List<TaxonomySave> taxonomyList)
			throws ApiException;

	public TaxonomyDefinition save(TaxonomyDefinition taxonomyDefinition);

	public Map<String, Object> uploadFile(HttpServletRequest request, FormDataMultiPart multiPart)
			throws IOException, ApiException, InterruptedException, ExecutionException;

	public TaxonomicNames findSynonymCommonName(Long taxonId);

	public List<TaxonomyDefinition> updateAddSynonym(HttpServletRequest request, Long speciesId, Long taxonId,
			SynonymData synonymData);

	public List<TaxonomyDefinition> deleteSynonym(HttpServletRequest request, Long speciesId, Long taxonId,
			Long synonymId);

	public TaxonomySearch getByNameSearch(String scientificName, String rankName) throws ApiException;

	public TaxonomyDefinition updateName(Long taxonId, String taxonName) throws ApiException;

	public TaxonomyDefinition updateStatus(HttpServletRequest request, TaxonomyStatusUpdate taxonomyStatusUpdate)
			throws ApiException, TaxonCreationException;

	public TaxonomyDefinition updatePosition(HttpServletRequest request, TaxonomyPositionUpdate taxonomyPositionUpdate);

	public TaxonomyNameListResponse getTaxonomyNameList(Long taxonId, Long classificationId, String rankList,
			String statusList, String positionList, Integer limit, Integer offset) throws IOException;
}
