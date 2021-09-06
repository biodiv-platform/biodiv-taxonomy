
/**
 * 
 */
package com.strandls.taxonomy.dao;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 * @author Abhishek Rudra
 *
 */
public class TaxonomyDaoModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(TaxonomyDefinitionDao.class).in(Scopes.SINGLETON);
		bind(TaxonomyRegistryDao.class).in(Scopes.SINGLETON);
		bind(SpeciesGroupMappingDao.class).in(Scopes.SINGLETON);
		bind(SpeciesGroupDao.class).in(Scopes.SINGLETON);
		bind(AcceptedSynonymDao.class).in(Scopes.SINGLETON);
		bind(SpeciesPermissionDao.class).in(Scopes.SINGLETON);
		bind(RankDao.class).in(Scopes.SINGLETON);
		bind(CommonNameDao.class).in(Scopes.SINGLETON);
		bind(SpeciesPermissionRequestDao.class).in(Scopes.SINGLETON);
	}
}
