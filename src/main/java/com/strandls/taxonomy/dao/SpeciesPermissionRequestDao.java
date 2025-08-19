/** */
package com.strandls.taxonomy.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strandls.taxonomy.TreeRoles;
import com.strandls.taxonomy.pojo.SpeciesPermissionRequest;
import com.strandls.taxonomy.util.AbstractDAO;

import jakarta.inject.Inject;

/**
 * @author Abhishek Rudra
 */
public class SpeciesPermissionRequestDao extends AbstractDAO<SpeciesPermissionRequest, Long> {

	private final Logger logger = LoggerFactory.getLogger(SpeciesPermissionRequestDao.class);

	@Inject
	protected SpeciesPermissionRequestDao(SessionFactory sessionFactory) {
		super(sessionFactory);
	}

	@Override
	public SpeciesPermissionRequest findById(Long id) {
		Session session = sessionFactory.openSession();
		SpeciesPermissionRequest result = null;
		try {
			result = session.get(SpeciesPermissionRequest.class, id);
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public SpeciesPermissionRequest requestPermissionExist(Long userId, Long taxonId, TreeRoles roles) {
		String qry = "from SpeciesPermissionRequest where userId = :userId and taxonConceptId = :taxonId and role = :role ";
		Session session = sessionFactory.openSession();
		SpeciesPermissionRequest result = null;
		try {
			Query<SpeciesPermissionRequest> query = session.createQuery(qry);
			query.setParameter("userId", userId);
			query.setParameter("taxonId", taxonId);
			query.setParameter("role", roles.getValue());
			result = query.getSingleResult();

		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return result;
	}
}
