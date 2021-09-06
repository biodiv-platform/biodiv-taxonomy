package com.strandls.taxonomy.util;

import static org.hibernate.type.StandardBasicTypes.LONG;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.query.Query;

public abstract class AbstractDAO<T, K extends Serializable> {

	protected SessionFactory sessionFactory;

	protected Class<? extends T> daoType;

	@SuppressWarnings("unchecked")
	protected AbstractDAO(SessionFactory sessionFactory) {
		daoType = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		this.sessionFactory = sessionFactory;
	}

	public T save(T entity) {
		Session session = sessionFactory.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			session.save(entity);
			tx.commit();
		} catch (Exception e) {
			if (tx != null)
				tx.rollback();
			throw e;
		} finally {
			session.close();
		}
		return entity;
	}

	public T update(T entity) {
		Session session = sessionFactory.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			session.update(entity);
			tx.commit();
		} catch (Exception e) {
			if (tx != null)
				tx.rollback();
			throw e;
		} finally {
			session.close();
		}
		return entity;
	}

	public T delete(T entity) {
		Session session = sessionFactory.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			session.delete(entity);
			tx.commit();
		} catch (Exception e) {
			if (tx != null)
				tx.rollback();
			throw e;
		} finally {
			session.close();
		}
		return entity;
	}

	/**
	 * Get complete count the given entity
	 * 
	 * @return
	 */

	public Long getRowCount() {
		Session session = sessionFactory.openSession();
		CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
		CriteriaQuery<Long> criteria = criteriaBuilder.createQuery(Long.class);
		CriteriaQuery<Long> count = criteria.select(criteriaBuilder.count(criteria.from(daoType)));
		Long rowCount = session.createQuery(count).getSingleResult();
		session.close();
		return rowCount;
	}

	/**
	 * Get count for the native query
	 * 
	 * @param queryString
	 * @param parameters
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Long getRowCount(String qryString, Map<String, Object> parameters) {
		Session session = sessionFactory.openSession();
		String queryString = "select count(*) from ( " + qryString + ") C";
		Query<Long> countQuery = session.createNativeQuery(queryString).addScalar("count", LONG);

		for (Map.Entry<String, Object> e : parameters.entrySet()) {
			countQuery.setParameter(e.getKey(), e.getValue());
		}

		Long count = countQuery.getSingleResult();
		session.close();
		return count;
	}

	/**
	 * Get count for the native query
	 * 
	 * @param queryString
	 * @param parameters
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<T> getByQueryString(String queryString, Map<String, Object> parameters, int limit, int offset) {
		Session session = sessionFactory.openSession();
		Query<T> query = session.createQuery(queryString);

		for (Map.Entry<String, Object> e : parameters.entrySet()) {
			query.setParameter(e.getKey(), e.getValue());
		}

		query.setMaxResults(limit).setFirstResult(offset);
		
		List<T> result = query.getResultList();
		session.close();
		return result;
	}

	public abstract T findById(K id);

	@SuppressWarnings({ "unchecked", "deprecation" })
	public List<T> findAll() {
		Session session = sessionFactory.openSession();
		Criteria criteria = session.createCriteria(daoType);
		List<T> entities = criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY).list();
		session.close();
		return entities;
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	public List<T> findAll(int limit, int offset) {
		Session session = sessionFactory.openSession();
		Criteria criteria = session.createCriteria(daoType).setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		List<T> entities = criteria.setFirstResult(offset).setMaxResults(limit).list();
		session.close();
		return entities;
	}

	public static <T> T map(Class<T> type, Object[] tuple) {
		List<Class<?>> tupleTypes = new ArrayList<>();
		for (Object field : tuple) {
			tupleTypes.add(field.getClass());
		}
		try {
			Constructor<T> ctor = type.getConstructor(tupleTypes.toArray(new Class<?>[tuple.length]));
			return ctor.newInstance(tuple);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> List<T> map(Class<T> type, List<Object[]> records) {
		List<T> result = new LinkedList<>();
		for (Object[] record1 : records) {
			result.add(map(type, record1));
		}
		return result;
	}

	@SuppressWarnings("rawtypes")
	public static <T> List<T> getResultList(Query query, Class<T> type) {
		@SuppressWarnings("unchecked")
		List<Object[]> records = query.getResultList();
		return map(type, records);
	}
}
