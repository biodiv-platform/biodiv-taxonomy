/** */
package com.strandls.taxonomy.dao;

import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strandls.taxonomy.pojo.AcceptedSynonym;
import com.strandls.taxonomy.util.AbstractDAO;

import jakarta.inject.Inject;

/**
 * @author Abhishek Rudra
 */
public class AcceptedSynonymDao extends AbstractDAO<AcceptedSynonym, Long> {

	private final Logger logger = LoggerFactory.getLogger(AcceptedSynonymDao.class);

	private static final String ACCEPTED_ID = "acceptedId";
	private static final String SYNONYM_ID = "synonymId";

	/**
	 * @param sessionFactory
	 */
	@Inject
	protected AcceptedSynonymDao(SessionFactory sessionFactory) {
		super(sessionFactory);
	}

	@Override
	public AcceptedSynonym findById(Long id) {
		Session session = sessionFactory.openSession();
		AcceptedSynonym result = null;
		try {
			result = session.get(AcceptedSynonym.class, id);
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return result;
	}

	public AcceptedSynonym createAcceptedSynonym(Long acceptedId, Long synonymId) {
		AcceptedSynonym acceptedSynonym = findByAccpetedIdSynonymId(acceptedId, synonymId);
		if (acceptedSynonym == null) {
			acceptedSynonym = new AcceptedSynonym();
			acceptedSynonym.setAcceptedId(acceptedId);
			acceptedSynonym.setSynonymId(synonymId);
			acceptedSynonym.setVersion(0L);
			save(acceptedSynonym);
		}
		return acceptedSynonym;
	}

	@SuppressWarnings("unchecked")
	public List<AcceptedSynonym> findBySynonymId(Long synonymId) {
		Session session = sessionFactory.openSession();
		List<AcceptedSynonym> result = null;
		String qry = "from AcceptedSynonym where synonymId = :synonymId";
		try {
			Query<AcceptedSynonym> query = session.createQuery(qry);
			query.setParameter(SYNONYM_ID, synonymId);
			query.setMaxResults(1);
			result = query.getResultList();
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public AcceptedSynonym findByAccpetedIdSynonymId(Long acceptedId, Long synonymId) {
		Session session = sessionFactory.openSession();
		AcceptedSynonym result = null;
		String qry = "from AcceptedSynonym where acceptedId = :acceptedId and synonymId = :synonymId";
		try {
			Query<AcceptedSynonym> query = session.createQuery(qry);
			query.setParameter(ACCEPTED_ID, acceptedId);
			query.setParameter(SYNONYM_ID, synonymId);
			query.setMaxResults(1);
			result = query.getSingleResult();
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public List<AcceptedSynonym> findByAccepetdId(Long acceptedId) {
		String qry = "from AcceptedSynonym where acceptedId = :acceptedId";
		Session session = sessionFactory.openSession();
		List<AcceptedSynonym> result = null;
		try {
			Query<AcceptedSynonym> query = session.createQuery(qry);
			query.setParameter(ACCEPTED_ID, acceptedId);
			result = query.getResultList();

		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public List<Long> findSynonymIdsByAcceptedIds(List<Long> acceptedIds) {
		if (acceptedIds == null || acceptedIds.isEmpty()) {
			return Collections.emptyList();
		}

		String qry = "select a.synonymId from AcceptedSynonym a where a.acceptedId in :acceptedIds";
		Session session = sessionFactory.openSession();
		List<Long> result = Collections.emptyList();

		try {
			Query<Long> query = session.createQuery(qry, Long.class);
			query.setParameter("acceptedIds", acceptedIds);
			result = query.getResultList();

		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return result;
	}

	/**
	 * Transfer all the synonym from one accepted name to another
	 *
	 * @param taxonId
	 * @param newTaxonId
	 */
	@SuppressWarnings("unchecked")
	public int synonymTransfer(Long taxonId, Long newTaxonId) {
		Session session = sessionFactory.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			Query<AcceptedSynonym> query = session.createNamedQuery("synonymTransfer");
			query.setParameter(ACCEPTED_ID, taxonId);
			query.setParameter("newAcceptedId", newTaxonId);
			int rowsUpdated = query.executeUpdate();
			tx.commit();
			return rowsUpdated;
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return 0;
	}

	@SuppressWarnings("unchecked")
	public int allSynonymTransfer(Long prevTaxonId, Long newTaxonId) {
		Session session = sessionFactory.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();

			Query query = session.createNamedQuery("synonymTransfer");
			query.setParameter("acceptedId", prevTaxonId);
			query.setParameter("newAcceptedId", newTaxonId);
			int rowsUpdated = query.executeUpdate();
			logger.debug(rowsUpdated + " Synonyms updated their accepted id");

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
	public int bulkSynonymTransfer(List<Long> synonymIds, Long newTaxonId) {
		Session session = sessionFactory.openSession();
		Transaction tx = null;

		try {
			tx = session.beginTransaction();

			String qry = "update AcceptedSynonym set acceptedId = :newAcceptedId where synonymId IN (:synonymIds)";
			Query<AcceptedSynonym> query = session.createQuery(qry);
			query.setParameterList("synonymIds", synonymIds);
			query.setParameter("newAcceptedId", newTaxonId);

			int rowsUpdated = query.executeUpdate();

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
}
