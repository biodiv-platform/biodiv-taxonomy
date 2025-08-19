package com.strandls.taxonomy.util;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.type.StandardBasicTypes;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

public abstract class AbstractDAO<T, K extends Serializable> {

	protected final SessionFactory sessionFactory;
	protected final Class<T> daoType;

	@SuppressWarnings("unchecked")
	protected AbstractDAO(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
		this.daoType = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}

	public T save(T entity) {
		try (Session session = sessionFactory.openSession()) {
			Transaction tx = session.beginTransaction();
			try {
				session.save(entity);
				tx.commit();
			} catch (Exception e) {
				tx.rollback();
				throw e;
			}
		}
		return entity;
	}

	public T update(T entity) {
		try (Session session = sessionFactory.openSession()) {
			Transaction tx = session.beginTransaction();
			try {
				session.update(entity);
				tx.commit();
			} catch (Exception e) {
				tx.rollback();
				throw e;
			}
		}
		return entity;
	}

	public T delete(T entity) {
		try (Session session = sessionFactory.openSession()) {
			Transaction tx = session.beginTransaction();
			try {
				session.delete(entity);
				tx.commit();
			} catch (Exception e) {
				tx.rollback();
				throw e;
			}
		}
		return entity;
	}

	public abstract T findById(K id);

	public List<T> findAll() {
		try (Session session = sessionFactory.openSession()) {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<T> cq = cb.createQuery(daoType);
			Root<T> root = cq.from(daoType);
			cq.select(root).distinct(true);
			return session.createQuery(cq).getResultList();
		}
	}

	public List<T> findAll(int limit, int offset) {
		try (Session session = sessionFactory.openSession()) {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<T> cq = cb.createQuery(daoType);
			Root<T> root = cq.from(daoType);
			cq.select(root).distinct(true);
			return session.createQuery(cq).setFirstResult(offset).setMaxResults(limit).getResultList();
		}
	}

	public Long getRowCount() {
		try (Session session = sessionFactory.openSession()) {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Long> cq = cb.createQuery(Long.class);
			cq.select(cb.count(cq.from(daoType)));
			return session.createQuery(cq).getSingleResult();
		}
	}

	public Long getRowCount(String queryString, Map<String, Object> parameters) {
		try (Session session = sessionFactory.openSession()) {
			String countQueryStr = "SELECT COUNT(*) AS count FROM (" + queryString + ") AS C";
			NativeQuery<Long> countQuery = session.createNativeQuery(countQueryStr).addScalar("count",
					StandardBasicTypes.LONG);
			parameters.forEach(countQuery::setParameter);
			return countQuery.getSingleResult();
		}
	}

	public List<T> getByQueryString(String hql, Map<String, Object> parameters, int limit, int offset) {
		try (Session session = sessionFactory.openSession()) {
			Query<T> query = session.createQuery(hql, daoType);
			parameters.forEach(query::setParameter);
			query.setFirstResult(offset).setMaxResults(limit);
			return query.getResultList();
		}
	}

	// Mapping utility: convert Object[] tuples into POJOs using constructor
	// injection
	public static <T> T map(Class<T> type, Object[] tuple) {
		List<Class<?>> types = new ArrayList<>();
		for (Object obj : tuple) {
			types.add(obj.getClass());
		}
		try {
			Constructor<T> ctor = type.getConstructor(types.toArray(new Class<?>[0]));
			return ctor.newInstance(tuple);
		} catch (Exception e) {
			throw new RuntimeException("Error mapping tuple to class " + type.getName(), e);
		}
	}

	public static <T> List<T> map(Class<T> type, List<Object[]> records) {
		List<T> result = new LinkedList<>();
		for (Object[] record : records) {
			result.add(map(type, record));
		}
		return result;
	}

	public static <T> List<T> getResultList(Query<?> query, Class<T> type) {
		@SuppressWarnings("unchecked")
		List<Object[]> records = (List<Object[]>) query.getResultList();
		return map(type, records);
	}
}
