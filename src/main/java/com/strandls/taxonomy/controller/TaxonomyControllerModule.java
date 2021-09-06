/**
 * 
 */
package com.strandls.taxonomy.controller;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 * @author Abhishek Rudra
 *
 */
public class TaxonomyControllerModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(TaxonomyDefinitionController.class).in(Scopes.SINGLETON);
		bind(RankController.class).in(Scopes.SINGLETON);
		bind(SpeciesGroupController.class).in(Scopes.SINGLETON);
		bind(TaxonomyRegistryController.class).in(Scopes.SINGLETON);
		bind(CommonNameController.class).in(Scopes.SINGLETON);
		bind(TaxonomyPermissionController.class).in(Scopes.SINGLETON);
	}
}
