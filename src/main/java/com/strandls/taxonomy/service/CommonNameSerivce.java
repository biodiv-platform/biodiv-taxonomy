/**
 * 
 */
package com.strandls.taxonomy.service;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.strandls.taxonomy.pojo.CommonName;
import com.strandls.taxonomy.pojo.CommonNamesData;

/**
 * 
 * @author vilay
 *
 */
public interface CommonNameSerivce {

	public CommonName fetchById(Long id);

	public CommonName save(CommonName commonName);

	public List<CommonName> getCommonName(Long languageId, Long taxonConceptId, String commonNameString);

	public CommonName updateIsPreffered(Long id);

	public List<CommonName> addCommonNames(Long taxonConceptId, Map<Long, String[]> languageIdToCommonNames,
			String source);

	public List<CommonName> updateAddCommonName(HttpServletRequest request, Long speciesId,
			CommonNamesData commonNamesData);

	public List<CommonName> removeCommonName(HttpServletRequest request, Long speciesId, Long commonNameId);

	public List<CommonName> fetchCommonNameWithLangByTaxonId(Long taxonId);

	public CommonName getPrefferedCommonName(Long taxonId);

	public List<CommonName> fetchByTaxonId(Long taxonId);

}
