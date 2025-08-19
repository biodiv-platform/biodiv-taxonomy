package com.strandls.taxonomy.util;

import java.util.List;

public abstract class AbstractService<T> {
	protected AbstractDAO<T, Long> dao;

	protected AbstractService(AbstractDAO<T, Long> dao) {
		this.dao = dao;
	}

	public T save(T entity) {
		this.dao.save(entity);
		return entity;
	}

	public T update(T entity) {
		this.dao.update(entity);
		return entity;
	}

	public T delete(Long id) {
		T entity = this.dao.findById(id);
		this.dao.delete(entity);
		return entity;
	}

	public T findById(Long id) {
		return this.dao.findById(id);
	}

	public List<T> findAll(int limit, int offset) {
		return this.dao.findAll(limit, offset);
	}

	public List<T> findAll() {
		return this.dao.findAll();
	}
}
