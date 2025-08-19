/** */
package com.strandls.taxonomy.dao;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strandls.taxonomy.pojo.Rank;
import com.strandls.taxonomy.util.AbstractDAO;

import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;

/**
 * @author vilay
 */
public class RankDao extends AbstractDAO<Rank, Long> {

	private final Logger logger = LoggerFactory.getLogger(RankDao.class);

	/**
	 * @param sessionFactory
	 */
	@Inject
	protected RankDao(SessionFactory sessionFactory) {
		super(sessionFactory);
	}

	@Override
	public Rank findById(Long id) {
		Session session = sessionFactory.openSession();
		Rank entity = null;
		try {
			entity = session.get(Rank.class, id);
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return entity;
	}

	public List<Rank> getAllRank() {
		return getAllRank(false);
	}

	public List<Rank> getAllRank(boolean topToBottom) {
		String queryStr;
		if (topToBottom)
			queryStr = "from Rank t where isDeleted = false order by rankValue";
		else
			queryStr = "from Rank t where isDeleted = false order by rankValue desc";
		try (Session session = sessionFactory.openSession()) {
			Query<Rank> query = session.createQuery(queryStr, Rank.class);
			try {
				return query.getResultList();
			} catch (NoResultException e) {
				return new ArrayList<>();
			}
		}
	}

	public Rank findRankByName(String rankName) {
		String queryStr = "from Rank t where t.name = :value and isDeleted = false";
		try (Session session = sessionFactory.openSession()) {
			Query<Rank> query = session.createQuery(queryStr, Rank.class);
			query.setParameter("value", rankName);
			try {
				return query.getSingleResult();
			} catch (NoResultException e) {
				return null;
			}
		}
	}
}
