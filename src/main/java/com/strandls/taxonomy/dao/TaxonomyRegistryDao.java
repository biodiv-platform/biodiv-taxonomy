/**
 * 
 */
package com.strandls.taxonomy.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strandls.taxonomy.pojo.TaxonomyDefinition;
import com.strandls.taxonomy.pojo.TaxonomyRegistry;
import com.strandls.taxonomy.pojo.response.TaxonRelation;
import com.strandls.taxonomy.pojo.response.TaxonomyRegistryResponse;
import com.strandls.taxonomy.util.AbstractDAO;

/**
 * @author Abhishek Rudra
 *
 */
public class TaxonomyRegistryDao extends AbstractDAO<TaxonomyRegistry, Long> {

	private final Logger logger = LoggerFactory.getLogger(TaxonomyRegistryDao.class);

	private static Long CLASSIFICATION_ID;

	static {
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");
		Properties properties = new Properties();
		try {
			properties.load(in);
		} catch (IOException e) {
		}
		CLASSIFICATION_ID = Long.parseLong(properties.getProperty("classificationId"));
	}
	
	public static Long getDefaultClassificationId() {
		return CLASSIFICATION_ID;
	}

	/**
	 * @param sessionFactory
	 */
	@Inject
	protected TaxonomyRegistryDao(SessionFactory sessionFactory) {
		super(sessionFactory);

	}

	@Override
	public TaxonomyRegistry findById(Long id) {
		Session session = sessionFactory.openSession();
		TaxonomyRegistry entity = null;
		try {
			entity = session.get(TaxonomyRegistry.class, id);

		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return entity;
	}

	@SuppressWarnings("unchecked")
	public TaxonomyRegistry findbyTaxonomyId(Long taxonomyId, Long classificationId) {

		classificationId = classificationId == null ? CLASSIFICATION_ID : classificationId;

		String qry = "from TaxonomyRegistry tr where tr.taxonomyDefinationId = :taxonomyId "
				+ "and tr.classificationId = :classificationId";
		Session session = sessionFactory.openSession();
		TaxonomyRegistry result = null;
		try {
			Query<TaxonomyRegistry> query = session.createQuery(qry);
			query.setParameter("taxonomyId", taxonomyId);
			query.setParameter("classificationId", classificationId);
			result = query.getSingleResult();
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}

		return result;
	}

	public TaxonomyRegistry createRegistry(Long classificationId, String path, String rank, Long taxonDefinitionId,
			Long uploaderId, Timestamp uploadTime) {
		uploadTime = uploadTime == null ? new Timestamp(new Date().getTime()) : uploadTime;

		classificationId = classificationId == null ? getDefaultClassificationId() : classificationId;
		TaxonomyRegistry registry = new TaxonomyRegistry();
		registry.setClassificationId(classificationId);
		registry.setPath(path);
		registry.setTaxonomyDefinationId(taxonDefinitionId);
		registry.setRank(rank);
		registry.setUploaderId(uploaderId);
		registry.setUploadTime(uploadTime);

		return save(registry);
	}

	public List<String> findByTaxonIdOnTraitList(List<Long> traitTaxonIds, Set<String> speciesGroupTaxonIds) {
		return findByTaxonIdOnTraitList(traitTaxonIds, speciesGroupTaxonIds, CLASSIFICATION_ID);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<String> findByTaxonIdOnTraitList(List<Long> traitTaxonIds, Set<String> speciesGroupTaxonIds,
			Long classificationId) {

		if (speciesGroupTaxonIds.isEmpty())
			return new ArrayList<>();
		String speciesGroupTaxons = String.join("|", speciesGroupTaxonIds);
		speciesGroupTaxons = "*." + speciesGroupTaxons + ".*";

		classificationId = classificationId == null ? CLASSIFICATION_ID : classificationId;

		String queryString = "select taxon_definition_id from taxonomy_registry t where t.classification_id = :classificationId and "
				+ "t.path @> any(select tr.path from taxonomy_registry tr where tr.taxon_definition_id in (:traitTaxonIds) "
				+ "and tr.path ~ lquery(:speciesGroupTaxons) and classification_id = :classificationId)";

		Session session = sessionFactory.openSession();

		List<String> result = new ArrayList<>();
		try {
			NativeQuery query = session.createNativeQuery(queryString);
			query.setParameter("classificationId", classificationId);
			query.setParameterList("traitTaxonIds", traitTaxonIds);
			query.setParameter("speciesGroupTaxons", speciesGroupTaxons);
			result = query.getResultList();
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return result;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<TaxonRelation> list(Long parent, List<Long> taxonIds, boolean expandTaxon, Long classificationId) {
		Session session = sessionFactory.openSession();
		String queryString = "";
		Query query;
		if (parent == null && taxonIds == null) {
			queryString = "select t.id,t.name, t.rank, ltree2text(tR.path) as path, tR.classification_id as classification, "
					+ " case when nlevel(tR.path) > 1 THEN ltree2text(subpath(tR.path,-2,1)) else null end as parent, t.position "
					+ " from taxonomy_definition as t, taxonomy_registry as tR"
					+ " where t.id=tR.taxon_definition_id and t.is_deleted=false  and tR.classification_id=:classification_id and "
					+ " nlevel(tR.path) = 2" + " order by nlevel(tR.path), t.name";
			query = session.createNativeQuery(queryString).setResultSetMapping("TaxonomyRelation");
		} else {
			String parentCheck = " ";
			if (expandTaxon && taxonIds != null && !taxonIds.isEmpty()) {
				List<String> allTaxonIds = getPathToRoot(taxonIds, classificationId);
				parentCheck = String.join("|", allTaxonIds);
				parentCheck = "*." + parentCheck + ".*{0,1}";
			} else if (parent != null) {
				parentCheck = "*." + parent + ".*{1}";
			} else {
				session.close();
				return new ArrayList<>();
			}
			queryString = "select t.id, t.name, t.rank, ltree2text(tR.path) as path, tR.classification_id as classification, "
					+ " case when nlevel(tR.path) > 1 THEN ltree2text(subpath(tR.path,-2,1)) else null end as parent, t.position "
					+ " from taxonomy_definition as t, taxonomy_registry as tR"
					+ " where t.id=tR.taxon_definition_id and t.is_deleted=false  and tR.classification_id=:classification_id and "
					+ " tR.path ~ lquery(:parentCheck) and nlevel(tR.path) > 1 " + " order by nlevel(tR.path), t.name";
			query = session.createNativeQuery(queryString).setResultSetMapping("TaxonomyRelation");
			query.setParameter("parentCheck", parentCheck);
		}
		classificationId = classificationId == null ? CLASSIFICATION_ID : classificationId;
		query.setParameter("classification_id", classificationId);

		try {
			return query.getResultList();
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}

		return new ArrayList<>();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<String> getPathToRoot(List<Long> taxonIds, Long classificationId) {
		Session session = sessionFactory.openSession();
		try {
			String sqlString = "select cast(taxon_definition_id as varchar) from taxonomy_registry where path @> "
					+ "any(select path from taxonomy_registry where taxon_definition_id in (:taxonIds) and classification_id=:classificationId) and "
					+ "classification_id=:classificationId";
			Query query = session.createNativeQuery(sqlString);
			query.setParameter("taxonIds", taxonIds);

			classificationId = classificationId == null ? CLASSIFICATION_ID : classificationId;

			query.setParameter("classificationId", classificationId);
			return query.getResultList();
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return new ArrayList<>();
	}

	@SuppressWarnings("rawtypes")
	public List<TaxonomyRegistryResponse> getPathToRoot(Long taxonId, Long classificationId) {
		Session session = sessionFactory.openSession();
		try {
			String sqlString = "select cast(td.id as varchar), td.rank, td.name, td.canonical_form from (select * from taxonomy_registry where path @> "
					+ "(select path from taxonomy_registry where taxon_definition_id = :taxonId and classification_id=:classificationId) and "
					+ "classification_id=:classificationId) tr " + "left outer join taxonomy_definition td "
					+ "on td.id = tr.taxon_definition_id order by tr.path";
			Query query = session.createNativeQuery(sqlString);
			query.setParameter("taxonId", taxonId);
			classificationId = classificationId == null ? CLASSIFICATION_ID : classificationId;
			query.setParameter("classificationId", classificationId);
			return getResultList(query, TaxonomyRegistryResponse.class);
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return new ArrayList<>();
	}
	
	/**
	 * Code below this point is only for the migration purpose.
	 * @param taxonId
	 * @param classificationIds
	 * @return
	 */

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<TaxonomyRegistry> getOldHierarchy(Long taxonId, List<Long> classificationIds) {
		Session session = sessionFactory.openSession();
		try {
			String sqlString = "select id, classification_id, ltree2text(path) path, rank, taxon_definition_id, upload_time, uploader_id from taxonomy_registry_backup "
					+ " where taxon_definition_id = :taxonId"
					+ " and classification_id in (:classificationIds) order by classification_id desc";
			Query query = session.createNativeQuery(sqlString, TaxonomyRegistry.class);
			query.setParameter("taxonId", taxonId);
			query.setParameter("classificationIds", classificationIds);
			return query.getResultList();
		} catch (Exception e) {
			logger.error(e.getMessage());
			return new ArrayList<>();
		} finally {
			session.close();
		}
	}
	
	@SuppressWarnings("rawtypes")
	public List<TaxonomyRegistryResponse> getPathToRootForOldHierarchy(Long taxonId, Long classificationId) {
		Session session = sessionFactory.openSession();
		try {
			String sqlString = "select cast(td.id as varchar), td.rank, td.name, td.canonical_form from (select * from taxonomy_registry_backup where path @> "
					+ "(select path from taxonomy_registry_backup where taxon_definition_id = :taxonId and "
					+ "classification_id=:classificationId) and classification_id=:classificationId) tr " + "left outer join taxonomy_definition td "
					+ "on td.id = tr.taxon_definition_id order by tr.path";
			Query query = session.createNativeQuery(sqlString);
			query.setParameter("taxonId", taxonId);
			classificationId = classificationId == null ? CLASSIFICATION_ID : classificationId;
			query.setParameter("classificationId", classificationId);
			return getResultList(query, TaxonomyRegistryResponse.class);
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return new ArrayList<>();
	}
	
	@SuppressWarnings("unchecked")
	public List<TaxonomyRegistryResponse> getNameFromPath(String path) {
		Session session = sessionFactory.openSession();
		try {
			List<Long> taxonIds = new ArrayList<>();
			for(String s : path.split("\\."))
				taxonIds.add(Long.parseLong(s));
			
			String sqlString = "select cast(td.id as varchar), td.rank, td.name, td.canonical_form from taxonomy_definition td "
					+ " left join taxonomy_rank r on td.rank = r.name "
					+ " where td.id in (:taxonIds) order by r.rankvalue";
			Query<String> query = session.createNativeQuery(sqlString);
			query.setParameterList("taxonIds", taxonIds);
			return getResultList(query, TaxonomyRegistryResponse.class);
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return null;
	}

	/**
	 * 
	 * Order for choosing candidate is as follow
	 * 821 - Catalog of Life
	 * 820 - FishBase Taxonomy Hierarchy
	 * 819 - IUCN Taxonomy Hierarchy (2010)
	 * 818 - GBIF Taxonomy Hierarchy
	 * 817 - Author Contributed Taxonomy Hierarchy
	 * 265798 - Combined Taxonomy Hierarchy
	 * 
	 * If we found any duplicate candidate the we fall back to IBP hierarchy. so we are maintaining both the record
	 * Pulling out one of the hierarchy mentioned above in given order and IBP hierarchy
	 * 
	 * 265799 - IBP hierarchy
	 * 
	 * Query to execute is as follow
	 * 
	 * select * from taxonomy_registry_backup
	 * where taxon_definition_id = :taxonId and (classification_id = :classificationId or classification_id = (
	 * 		select classification_id from taxonomy_registry_backup
	 * 		where taxon_definition_id = :taxonId
	 * 		group by classification_id
	 * 		order by case classification_id
	 * 			when 821 then 1 
	 * 			when 820 then 2	
	 * 			when 819 then 3
	 * 			when 818 then 4	
	 * 			when 817 then 5
	 * 			when 265798 then 6
	 * 			end limit 1
	 * )) order by classification_id desc
	 * @param taxonId
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<TaxonomyRegistry> getSnappingCandidates(Long taxonId) {
		Session session = sessionFactory.openSession();
		Long classificationId = getDefaultClassificationId();
		try {
			String sqlString = "select * from taxonomy_registry_backup "
					+ " where taxon_definition_id =:taxonId and (classification_id =:classificationId or classification_id = ( "
					+ " select classification_id from taxonomy_registry_backup "
					+ " where taxon_definition_id = :taxonId "
					+ " group by classification_id "
					+ " order by case classification_id "
						+ " when 821 then 1"
						+ "	when 820 then 2"
						+ "	when 819 then 3"
						+ "	when 818 then 4"
						+ "	when 817 then 5"
						+ "	when 265798 then 6"
						+ " else 7 end limit 1"
					+ ")) order by classification_id desc";
			Query query = session.createNativeQuery(sqlString, TaxonomyRegistry.class);
			query.setParameter("taxonId", taxonId);
			query.setParameter("classificationId", classificationId);
			return query.getResultList();
		} catch (Exception e) {
			logger.error(e.getMessage());
			return new ArrayList<>();
		} finally {
			session.close();
		}
	}

	@SuppressWarnings("rawtypes")
	public TaxonomyRegistry getParentToSnapOn(String path) {
		Session session = sessionFactory.openSession();
		Long classificationId = getDefaultClassificationId();
		try {
			List<String> paths = Arrays.asList(path.split("\\."));
			String parentCheck = String.join("|", paths);
			parentCheck = "*." + parentCheck;
			
			String sqlString = "select * from taxonomy_registry tr "
					+ " left join taxonomy_rank r on tr.rank = r.name "
					+ " where tr.path ~ lquery(:parentCheck) and tr.classification_id = :classificationId "
					+ " order by r.rankvalue desc, nlevel(path) desc";
			Query query = session.createNativeQuery(sqlString, TaxonomyRegistry.class).setMaxResults(1);
			query.setParameter("parentCheck", parentCheck);
			query.setParameter("classificationId", classificationId);
			return (TaxonomyRegistry) query.getSingleResult();
		} catch (Exception e) {
			return null;
		} finally {
			session.close();
		}
	}

	public TaxonomyDefinition findChildWithNotAssigned(Long parentId, Long classificationId) {
		Session session = sessionFactory.openSession();
		classificationId = classificationId == null ? CLASSIFICATION_ID : classificationId;
		try {

			String parentCheck = "*." + parentId + ".*{1}";
			
			String sqlString = "select td.* "
					+ "from (select * from taxonomy_registry where path ~ lquery(:parentCheck) and classification_id = :classificationId) tr "
					+ "left join (select * from taxonomy_definition where name like 'Not assigned') td "
					+ "on td.id = tr.taxon_definition_id "
					+ "where td.id is not null";
			Query<TaxonomyDefinition> query = session.createNativeQuery(sqlString, TaxonomyDefinition.class).setMaxResults(1);
			query.setParameter("parentCheck", parentCheck);
			query.setParameter("classificationId", classificationId);
			List<TaxonomyDefinition> taxonList = query.getResultList();
			if(taxonList.isEmpty())
				return null;
			else
				return taxonList.get(0);
		} catch (Exception e) {
			return null;
		} finally {
			session.close();
		}
	}

}
