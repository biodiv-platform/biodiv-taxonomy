/**
 * 
 */
package com.strandls.taxonomy.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strandls.taxonomy.TreeRoles;
import com.strandls.taxonomy.pojo.SpeciesPermission;
import com.strandls.taxonomy.util.AbstractDAO;
import com.strandls.taxonomy.util.TaxonomyUtil;

/**
 * @author Abhishek Rudra
 *
 */
public class SpeciesPermissionDao extends AbstractDAO<SpeciesPermission, Long> {

	private final Logger logger = LoggerFactory.getLogger(SpeciesPermissionDao.class);
	private static final String USER_ID = "userId";

	private Map<TreeRoles, Long> roleIdMap = TaxonomyUtil.getRoleIdMap();

	/**
	 * @param sessionFactory
	 */
	@Inject
	protected SpeciesPermissionDao(SessionFactory sessionFactory) {
		super(sessionFactory);
	}

	@Override
	public SpeciesPermission findById(Long id) {
		Session session = sessionFactory.openSession();
		SpeciesPermission entity = null;
		try {
			entity = session.get(SpeciesPermission.class, id);
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return entity;
	}

	@SuppressWarnings("unchecked")
	public List<SpeciesPermission> findByUserId(Long userId) {

		String qry = "from SpeciesPermission where authorId = :userId";
		Session session = sessionFactory.openSession();
		List<SpeciesPermission> allowedTaxonList = new ArrayList<>();
		try {
			Query<SpeciesPermission> query = session.createQuery(qry);
			query.setParameter(USER_ID, userId);
			allowedTaxonList = query.getResultList();

		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return allowedTaxonList;

	}

	@SuppressWarnings("unchecked")
	public SpeciesPermission findPermissionOntaxon(Long userId, Long taxonId) {
		String qry = "from SpeciesPermission where authorId = :userId and taxonConceptId = :taxonId";
		Session session = sessionFactory.openSession();
		SpeciesPermission result = null;
		try {
			Query<SpeciesPermission> query = session.createQuery(qry);
			query.setParameter(USER_ID, userId);
			query.setParameter("taxonId", taxonId);
			result = query.getSingleResult();
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public Boolean checkPermission(Long userId, Long taxonId, TreeRoles role) {
		String qry = "from SpeciesPermission where authorId = :userId and taxonConceptId = :taxonId and permissionType = :role";
		Session session = sessionFactory.openSession();
		Boolean result = false;
		try {
			Query<SpeciesPermission> query = session.createQuery(qry);
			query.setParameter(USER_ID, userId);
			query.setParameter("taxonId", taxonId);
			query.setParameter("role", roleIdMap.get(role));
			SpeciesPermission dataRow = null;
			dataRow = query.getSingleResult();
			if (dataRow != null)
				result = true;
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return result;
	}

}
