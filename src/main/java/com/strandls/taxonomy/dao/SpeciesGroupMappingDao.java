/** */
package com.strandls.taxonomy.dao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strandls.taxonomy.pojo.SpeciesGroupMapping;
import com.strandls.taxonomy.util.AbstractDAO;

import jakarta.inject.Inject;

/**
 * @author Abhishek Rudra
 */
public class SpeciesGroupMappingDao extends AbstractDAO<SpeciesGroupMapping, Long> {

	private final Logger logger = LoggerFactory.getLogger(SpeciesGroupMappingDao.class);

	/**
	 * @param sessionFactory
	 */
	@Inject
	protected SpeciesGroupMappingDao(SessionFactory sessionFactory) {
		super(sessionFactory);
	}

	@Override
	public SpeciesGroupMapping findById(Long id) {
		Session session = sessionFactory.openSession();
		SpeciesGroupMapping entity = null;
		try {
			entity = session.get(SpeciesGroupMapping.class, id);
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return entity;
	}

	@SuppressWarnings("unchecked")
	public List<SpeciesGroupMapping> getTaxonomyId(Long sGroup) {

		String qry = "from SpeciesGroupMapping where speciesGroupId = :sGroup";
		Session session = sessionFactory.openSession();
		List<SpeciesGroupMapping> result = new ArrayList<>();
		try {
			Query<SpeciesGroupMapping> query = session.createQuery(qry);
			query.setParameter("sGroup", sGroup);
			result = query.getResultList();
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return result;
	}

	public Set<String> getTaxonIds(Long speciesGroupId) {
		String qry = "from SpeciesGroupMapping where speciesGroupId = :sGroup";
		Session session = sessionFactory.openSession();
		List<SpeciesGroupMapping> speciesGroupMappings = new ArrayList<>();
		try {
			Query<SpeciesGroupMapping> query = session.createQuery(qry, SpeciesGroupMapping.class);
			query.setParameter("sGroup", speciesGroupId);
			speciesGroupMappings = query.getResultList();
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		Set<String> result = new HashSet<>();
		for (SpeciesGroupMapping s : speciesGroupMappings) {
			result.add(s.getTaxonConceptId().toString());
		}
		return result;
	}
}
