/**
 * 
 */
package com.strandls.taxonomy.dao;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.hibernate.type.StandardBasicTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strandls.taxonomy.TaxonomyConfig;
import com.strandls.taxonomy.pojo.AcceptedSynonym;
import com.strandls.taxonomy.pojo.TaxonomyDefinition;
import com.strandls.taxonomy.pojo.TaxonomyRegistry;
import com.strandls.taxonomy.pojo.enumtype.TaxonomyPosition;
import com.strandls.taxonomy.pojo.enumtype.TaxonomyStatus;
import com.strandls.taxonomy.pojo.response.TaxonomyNameListResponse;
import com.strandls.taxonomy.pojo.response.TaxonomyNamelistItem;
import com.strandls.taxonomy.service.exception.TaxonCreationException;
import com.strandls.taxonomy.util.AbstractDAO;
import com.strandls.taxonomy.util.TaxonomyUtil;
import com.strandls.utility.pojo.ParsedName;

/**
 * @author Abhishek Rudra
 *
 */
public class TaxonomyDefinitionDao extends AbstractDAO<TaxonomyDefinition, Long> {

	private static final String TAXONOMY_NAMELIST_QUERY = "taxonomyNamelist.sql";

	private static final String TAXONOMY_NAMELIST_COUNT_QUERY = "taxonomyNamelistCount.sql";

	private final Logger logger = LoggerFactory.getLogger(TaxonomyDefinitionDao.class);

	/**
	 * @param sessionFactory
	 */
	@Inject
	protected TaxonomyDefinitionDao(SessionFactory sessionFactory) {
		super(sessionFactory);
	}

	@Override
	public TaxonomyDefinition findById(Long id) {
		Session session = sessionFactory.openSession();
		TaxonomyDefinition entity = null;
		try {
			entity = session.get(TaxonomyDefinition.class, id);
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return entity;
	}

	public Long getRowCount() {
		Session session = sessionFactory.openSession();
		CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
		CriteriaQuery<Long> criteria = criteriaBuilder.createQuery(Long.class);
		CriteriaQuery<Long> count = criteria.select(criteriaBuilder.count(criteria.from(TaxonomyDefinition.class)));
		Long rowCount = session.createQuery(count).getSingleResult();
		session.close();
		return rowCount;
	}

	public List<Long> getAllIds(int limit, int offset) {
		Session session = sessionFactory.openSession();
		CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
		CriteriaQuery<Long> criteria = criteriaBuilder.createQuery(Long.class);
		Root<TaxonomyDefinition> q = criteria.from(TaxonomyDefinition.class);

		criteria.select(q.get("id"));
		List<Long> ids = session.createQuery(criteria).setFirstResult(offset).setMaxResults(limit).getResultList();
		session.close();
		return ids;
	}

	@SuppressWarnings("unchecked")
	public List<TaxonomyDefinition> breadCrumbSearch(String path) {
		Session session = sessionFactory.openSession();
		List<TaxonomyDefinition> result = null;

		String qry = "from " + daoType.getSimpleName() + " t where t.id in(" + path + ")";// + " order by t.rank";
		try {
			Query<TaxonomyDefinition> query = session.createQuery(qry);
			result = query.getResultList();
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}

		return result;
	}

	public List<TaxonomyDefinition> findByCanonicalForm(String canonicalForm, String rankName) {
		String queryStr = "" + "from " + daoType.getSimpleName() + " t "
				+ "where t.canonicalForm = :canonicalForm and t.rank = :rank and isDeleted = false";
		try (Session session = sessionFactory.openSession()) {
			Query<TaxonomyDefinition> query = session.createQuery(queryStr, TaxonomyDefinition.class);
			query.setParameter("canonicalForm", canonicalForm);
			query.setParameter("rank", rankName);
			try {
				List<TaxonomyDefinition> result = query.getResultList();
				if (result == null || result.isEmpty())
					return new ArrayList<TaxonomyDefinition>();
				return result;
			} catch (NoResultException e) {
				return new ArrayList<TaxonomyDefinition>();
			}
		}
	}

	// TODO : Not used anywhere, Need to decide for deletion
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<Object[]> search(String term) {
		Session session = sessionFactory.openSession();
		try {
			String sqlString = "select t.name,t.status,t.position,t.id,t.rank from TaxonomyDefinition as t where lower(t.name) like :term order by t.name";
			Query query = session.createQuery(sqlString);
			query.setMaxResults(10);
			query.setParameter("term", term.toLowerCase().trim() + '%');
			return query.getResultList();
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<String> specificSearch(String term, Long taxonId) {
		Session session = sessionFactory.openSession();
		try {
			String sqlString = "select distinct(cast(case when t.status = 'SYNONYM' then a.accepted_id else t.id end as varchar)) as id"
					+ " from (select * from taxonomy_definition where " + (taxonId != null ? " id=:taxonId and " : "")
					+ " lower(name) like lower(:term)) t " + " left outer join accepted_synonym a "
					+ " on t.id = a.synonym_id order by id ";

			Query query = session.createNativeQuery(sqlString);
			query.setParameter("term", term.toLowerCase().trim());
			if (taxonId != null)
				query.setParameter("taxonId", taxonId);
			return query.getResultList();
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return null;
	}

	/**
	 * This code is to get the hierarchy of all the child of given taxonId
	 * 
	 * @param taxonId - input taxonomy id
	 * @return - hierarchy for all the children
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<Long> getAllChildren(Long taxonId) {

		Session session = sessionFactory.openSession();
		try {
			String sqlString = TaxonomyConfig.fetchFileAsString("treeChildren.sql");
			Query query = session.createNativeQuery(sqlString).addScalar("taxon_definition_id",
					StandardBasicTypes.LONG);
			if (taxonId != null)
				query.setParameter("taxonId", taxonId);
			return query.getResultList();
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	public int updateStatusToSynonymInDB(TaxonomyRegistry newTaxonomyRegistry, TaxonomyRegistry oldtaxonomyRegistry) {

		Long newTaxonId = newTaxonomyRegistry.getTaxonomyDefinationId();
		Long taxonId = oldtaxonomyRegistry.getTaxonomyDefinationId();

		Session session = sessionFactory.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();

			// Add new synonym and attach it to the given accepted taxonomy
			AcceptedSynonym acceptedSynonym = new AcceptedSynonym();
			acceptedSynonym.setAcceptedId(newTaxonId);
			acceptedSynonym.setSynonymId(taxonId);
			acceptedSynonym.setVersion(0L);
			session.save(acceptedSynonym);

			// Transfer synonym to the new Accepted name
			// String qry = "update AcceptedSynonym set acceptedId = :newAcceptedId where
			// acceptedId = :acceptedId";
			Query query = session.createNamedQuery("synonymTransfer");
			query.setParameter("acceptedId", taxonId);
			query.setParameter("newAcceptedId", newTaxonId);
			int rowsUpdated = query.executeUpdate();
			logger.debug(rowsUpdated + " Synonyms updated their accepted id");

			// Remove old taxonomy from the hierarchy.
			session.delete(oldtaxonomyRegistry);

			// Attach all the children to new accepted name (Hierarchy update)
			String newPath = newTaxonomyRegistry.getPath();
			String oldPath = oldtaxonomyRegistry.getPath();
			String qry = "update taxonomy_registry "
					+ " set path = text2ltree(:newPath) || subpath(path, nlevel(text2ltree(:oldPath)))"
					+ " where path <@ text2ltree(:oldPath) and path != text2ltree(:oldPath)";
			query = session.createNativeQuery(qry);
			query.setParameter("newPath", newPath);
			query.setParameter("oldPath", oldPath);
			rowsUpdated += query.executeUpdate();

			tx.commit();
			return rowsUpdated;
		} catch (Exception e) {
			if (tx != null)
				tx.rollback();
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return 0;
	}

	public TaxonomyDefinition createTaxonomyDefiniiton(ParsedName parsedName, String rankName,
			TaxonomyStatus taxonomyStatus, TaxonomyPosition taxonomyPosition, String source, String sourceId,
			Long uploaderId) throws TaxonCreationException {
		if (parsedName == null || parsedName.getCanonicalName() == null)
			throw new TaxonCreationException("Not valid name");
		String canonicalName = parsedName.getCanonicalName().getFull();
		String binomialName = TaxonomyUtil.getBinomialName(canonicalName);
		String italicisedForm = TaxonomyUtil.getItalicisedForm(parsedName, rankName);
		Timestamp uploadTime = new Timestamp(new Date().getTime());
		String status = taxonomyStatus.name();
		String position = taxonomyPosition.name();
		String classs = "species.TaxonomyDefinition";

		TaxonomyDefinition taxonomyDefinition = new TaxonomyDefinition();
		taxonomyDefinition.setBinomialForm(binomialName);
		taxonomyDefinition.setCanonicalForm(canonicalName);
		taxonomyDefinition.setItalicisedForm(italicisedForm);
		taxonomyDefinition.setName(parsedName.getVerbatim().trim());
		taxonomyDefinition.setNormalizedForm(parsedName.getNormalized());
		taxonomyDefinition.setRank(rankName);
		taxonomyDefinition.setUploadTime(uploadTime);
		taxonomyDefinition.setUploaderId(uploaderId);
		taxonomyDefinition.setStatus(status);
		taxonomyDefinition.setPosition(position);
		taxonomyDefinition.setClasss(classs);
		taxonomyDefinition.setViaDatasource(source);
		taxonomyDefinition.setNameSourceId(sourceId);
		taxonomyDefinition.setAuthorYear(parsedName.getAuthorship());
		taxonomyDefinition.setIsDeleted(false);
		taxonomyDefinition = save(taxonomyDefinition);
		return taxonomyDefinition;
	}

	@SuppressWarnings("unchecked")
	public TaxonomyNameListResponse getTaxonomyNameList(Long taxonId, Long classificationId, List<String> rankList,
			List<String> statusList, List<String> positionList, Integer limit, Integer offset) throws IOException {

		String qryString = TaxonomyConfig.fetchFileAsString(TAXONOMY_NAMELIST_QUERY);
		String countQueryString = TaxonomyConfig.fetchFileAsString(TAXONOMY_NAMELIST_COUNT_QUERY);
		
		Session session = sessionFactory.openSession();
		
		Query<Integer> countQuery = session.createNativeQuery(countQueryString).addScalar("count", StandardBasicTypes.INTEGER);
		countQuery.setParameter("taxonId", taxonId);
		countQuery.setParameter("classificationId", classificationId);
		countQuery.setParameter("rank", rankList);
		countQuery.setParameter("status", statusList);
		countQuery.setParameter("position", positionList);
		
		Integer count = countQuery.getSingleResult();
		
		Query<TaxonomyNamelistItem> query = session.createNativeQuery(qryString)
				.setResultSetMapping("TaxonomyNameList");
		
		classificationId = classificationId == null ? TaxonomyRegistryDao.getDefaultClassificationId()
				: classificationId;

		query.setParameter("taxonId", taxonId);
		query.setParameter("classificationId", classificationId);
		query.setParameter("rank", rankList);
		query.setParameter("status", statusList);
		query.setParameter("position", positionList);

		if (limit != -1 && offset != -1) {
			query.setMaxResults(limit);
			query.setFirstResult(offset);
		}

		List<TaxonomyNamelistItem> taxonomyNamelistItems = query.getResultList();

		TaxonomyNameListResponse response = new TaxonomyNameListResponse();
		response.setCount(count);
		response.setTaxonomyNameListItems(taxonomyNamelistItems);

		session.close();

		return response;
	}
}
