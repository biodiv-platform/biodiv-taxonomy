/** */
package com.strandls.taxonomy.dao;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strandls.taxonomy.pojo.SpeciesGroup;
import com.strandls.taxonomy.util.AbstractDAO;

import jakarta.inject.Inject;

/**
 * @author Abhishek Rudra
 */
public class SpeciesGroupDao extends AbstractDAO<SpeciesGroup, Long> {

	private final Logger logger = LoggerFactory.getLogger(SpeciesGroupDao.class);

	/**
	 * @param sessionFactory
	 */
	@Inject
	protected SpeciesGroupDao(SessionFactory sessionFactory) {
		super(sessionFactory);
	}

	@Override
	public SpeciesGroup findById(Long id) {
		Session session = sessionFactory.openSession();
		SpeciesGroup entity = null;
		try {
			entity = session.get(SpeciesGroup.class, id);
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}

		return entity;
	}

	@SuppressWarnings("unchecked")
	public List<SpeciesGroup> findAllOrdered() {
		Session session = sessionFactory.openSession();
		List<SpeciesGroup> result = null;

		String qry = "from SpeciesGroup order by groupOrder asc";
		try {
			Query<SpeciesGroup> query = session.createQuery(qry);
			result = query.getResultList();

		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public Long getGroupIdByTaxonId(Long taxonId) {
		Session session = sessionFactory.openSession();
		List<String> result = null;

		String qry = "select (select cast(species_group_id as varchar) from species_group_mapping where taxon_concept_id = tr2.taxon_definition_id) "
				+ "from (select path from taxonomy_registry where taxon_definition_id in (:taxonId)) tr1 "
				+ "inner join (select taxon_definition_id, path from taxonomy_registry where taxon_definition_id in "
				+ "(select taxon_concept_id from species_group_mapping)) tr2 " + "on tr2.path @> tr1.path";
		try {
			Query<String> query = session.createNativeQuery(qry);
			query.setParameter("taxonId", taxonId);
			result = query.getResultList();
			return Long.parseLong(result.get(0));
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			session.close();
		}
		return null;
	}
}
