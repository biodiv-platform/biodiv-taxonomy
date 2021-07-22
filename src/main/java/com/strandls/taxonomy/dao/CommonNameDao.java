/**
 * 
 */
package com.strandls.taxonomy.dao;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.NoResultException;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strandls.taxonomy.pojo.CommonName;
import com.strandls.taxonomy.util.AbstractDAO;

/**
 * 
 * @author vilay
 *
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
			String queryStr = "" + "from " + daoType.getSimpleName() + " t " + "where "
					+ (languageId == null ? "languageId is NULL" : "languageId = :languageId")
					+ " and taxonConceptId =:taxonConceptId and name =:name and isDeleted = false";
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

}
