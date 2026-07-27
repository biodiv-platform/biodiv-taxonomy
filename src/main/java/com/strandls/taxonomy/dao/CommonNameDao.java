/** */
package com.strandls.taxonomy.dao;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strandls.taxonomy.pojo.AcceptedSynonym;
import com.strandls.taxonomy.pojo.CommonName;
import com.strandls.taxonomy.util.AbstractDAO;

import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;

/**
 * @author vilay
 */
public class CommonNameDao extends AbstractDAO<CommonName, Long> {

	private final Logger logger = LoggerFactory.getLogger(CommonNameDao.class);

	/**
	 * @param sessionFactory
	 */
	@Inject
	protected CommonNameDao(SessionFactory sessionFactory) {
		super(sessionFactory);
	}

	@Override
	public CommonName findById(Long id) {
		Session session = sessionFactory.openSession();
		CommonName entity = null;
		try {
			entity = session.get(CommonName.class, id);
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return entity;
	}

	public List<CommonName> fetchByTaxonId(Long taxonId) {
		String queryStr = "from CommonName t where t.taxonConceptId = :taxonId order by id";
		Session session = sessionFactory.openSession();
		Query<CommonName> query = session.createQuery(queryStr, CommonName.class);
		query.setParameter("taxonId", taxonId);

		try {
			return query.getResultList();
		} finally {
			session.close();
		}
	}

	public List<CommonName> getCommonName(Long languageId, Long taxonConceptId, String commonNameString) {
		try (Session session = sessionFactory.openSession()) {
			String queryStr;

			if (languageId == null)
				queryStr = "from CommonName t where languageId is NULL and taxonConceptId =:taxonConceptId and name =:name and isDeleted = false";
			else
				queryStr = "from CommonName t where languageId = :languageId and taxonConceptId =:taxonConceptId and name =:name and isDeleted = false";

			Query<CommonName> query = session.createQuery(queryStr, CommonName.class);
			if (languageId != null)
				query.setParameter("languageId", languageId);
			query.setParameter("taxonConceptId", taxonConceptId);
			query.setParameter("name", commonNameString);
			return query.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		}
	}

	@SuppressWarnings("unchecked")
	public List<CommonName> findByTaxonId(Long taxonId) {
		String qry = "from CommonName where taxonConceptId = :taxonId and isDeleted = false";
		Session session = sessionFactory.openSession();
		List<CommonName> result = null;
		try {
			Query<CommonName> query = session.createQuery(qry);
			query.setParameter("taxonId", taxonId);
			result = query.getResultList();

		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public int bulkCommonNameTransfer(List<Long> commonNameIds, Long newTaxonId) {
		Session session = sessionFactory.openSession();
		Transaction tx = null;

		try {
			// START TRANSACTION
			tx = session.beginTransaction();

			String qry = "update CommonName set taxonConceptId = :newAcceptedId, isPreffered = false where id IN (:commonNameIds)";
			Query<AcceptedSynonym> query = session.createQuery(qry);
			query.setParameterList("commonNameIds", commonNameIds);
			query.setParameter("newAcceptedId", newTaxonId);

			int rowsUpdated = query.executeUpdate();

			// COMMIT TRANSACTION
			tx.commit();

			return rowsUpdated;

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return 0;
	}

	@SuppressWarnings("unchecked")
	public int allCommonNameTransfer(Long prevTaxonId, Long newTaxonId) {
		Session session = sessionFactory.openSession();
		Transaction tx = null;

		try {
			// START TRANSACTION
			tx = session.beginTransaction();

			String qry = "update CommonName set taxonConceptId = :newAcceptedId, isPreffered = false where taxonConceptId = :prevTaxonId";
			Query<AcceptedSynonym> query = session.createQuery(qry);
			query.setParameter("prevTaxonId", prevTaxonId);
			query.setParameter("newAcceptedId", newTaxonId);

			int rowsUpdated = query.executeUpdate();

			// COMMIT TRANSACTION
			tx.commit();

			return rowsUpdated;

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return 0;
	}

	@SuppressWarnings("unchecked")
	public List<CommonName> findByCommonNameIds(List<Long> commonNameIds) {
		String qry = "from CommonName where id IN (:commonNameIds)";
		Session session = sessionFactory.openSession();
		List<CommonName> result = null;
		try {
			Query<CommonName> query = session.createQuery(qry);
			query.setParameterList("commonNameIds", commonNameIds);
			result = query.getResultList();

		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return result;
	}
}
