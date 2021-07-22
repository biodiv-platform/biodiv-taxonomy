package com.strandls.taxonomy.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.strandls.authentication_utility.filter.ValidateUser;
import com.strandls.taxonomy.ApiConstants;
import com.strandls.taxonomy.pojo.response.BreadCrumb;
import com.strandls.taxonomy.pojo.response.TaxonRelation;
import com.strandls.taxonomy.pojo.response.TaxonTree;
import com.strandls.taxonomy.service.TaxonomyRegistryService;
import com.strandls.taxonomy.util.TaxonomyUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api("Taxonomy Tree Services")
@Path(ApiConstants.V1 + ApiConstants.TREE)
public class TaxonomyRegistryController {

	@Inject
	private TaxonomyRegistryService taxonomyRegistry;

	@GET
	@Path(ApiConstants.BREADCRUMB + "/{taxonomyId}")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)

	@ApiOperation(value = "Find Taxonomy Registry by ID", notes = "Returns Taxonomy Registry details", response = BreadCrumb.class, responseContainer = "List")
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Taxonomy not found", response = String.class) })

	public Response getTaxonomyBreadCrumb(@PathParam("taxonomyId") String taxonomyId) {
		try {

			Long id = Long.parseLong(taxonomyId);
			List<BreadCrumb> breadCrumbs = taxonomyRegistry.fetchByTaxonomyId(id);
			return Response.status(Status.OK).entity(breadCrumbs).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).build();
		}
	}

	@GET
	@Path(ApiConstants.BREADCRUMB)
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)

	@ValidateUser
	@ApiOperation(value = "Find taxon Tree for a list of Taxons", notes = "Returns a List of Taxon Tree", response = TaxonTree.class, responseContainer = "List")
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "unable to fetch the taxon Tree", response = String.class) })

	public Response getTaxonTree(@Context HttpServletRequest request,
			@ApiParam(name = "taxonList") @QueryParam("taxonList") String taxonList) {
		try {

			String[] taxList = taxonList.split(",");
			List<Long> tList = new ArrayList<Long>();
			for (String s : taxList) {
				tList.add(Long.parseLong(s.trim()));
			}
			List<TaxonTree> result = taxonomyRegistry.fetchTaxonTrees(tList);
			return Response.status(Status.OK).entity(result).build();

		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	/**
	 * 
	 * @param parent           dummy
	 * @param classificationId dummy
	 * @param taxonIds         dummy
	 * @param expand_taxon     dummy
	 * @return dummy method is responsible for displaying taxon list
	 */
	@GET
	@Path("/list")
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)

	@ApiOperation(value = "Get taxon relationship", notes = "Returns a taxon relationship", response = TaxonRelation.class, responseContainer = "List")
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "unable to fetch the taxon relationship", response = String.class) })
	public Response list(@QueryParam("parent") Long parent, @QueryParam("classification") Long classificationId,
			@QueryParam("taxonIds") String taxonIds,
			@DefaultValue("false") @QueryParam("expand_taxon") Boolean expandTaxon) {
		try {
			List<TaxonRelation> result = taxonomyRegistry.list(parent, taxonIds, expandTaxon, classificationId);
			return Response.status(Status.OK).entity(result).build();

		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}
	
	@POST
	@Path("/migrate/clean")
	@Produces(MediaType.APPLICATION_JSON)
	
	@ValidateUser

	@ApiOperation(value = "Migrate the taxonomy hierarchy", notes = "Migrate hierarchy with following order first take", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "unable to do migration", response = String.class) })
	public Response migrateCleanName(@Context HttpServletRequest request) {
		try {
			if(!TaxonomyUtil.isAdmin(request))
				throw new WebApplicationException(
						Response.status(Response.Status.UNAUTHORIZED).entity("Only admin can do migration").build());
			
			Map<String, Object> result = taxonomyRegistry.migrateCleanName();
			return Response.status(Status.OK).entity(result).build();

		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}
	
	@POST
	@Path("/migrate/working")
	@Produces(MediaType.APPLICATION_JSON)
	
	@ValidateUser

	@ApiOperation(value = "Migrate the taxonomy hierarchy for working name", notes = "Migrate hierarchy for working name", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "unable to do migration", response = String.class) })
	public Response migrateWorkingName(@Context HttpServletRequest request) {
		try {
			if(!TaxonomyUtil.isAdmin(request))
				throw new WebApplicationException(
						Response.status(Response.Status.UNAUTHORIZED).entity("Only admin can do migration").build());
			
			Map<String, Object> result = taxonomyRegistry.snapWorkingNames();
			return Response.status(Status.OK).entity(result).build();

		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}
	
	@POST
	@Path("/migrate/raw")
	@Produces(MediaType.APPLICATION_JSON)
	
	@ValidateUser

	@ApiOperation(value = "Migrate the taxonomy hierarchy for raw name", notes = "Migrate hierarchy for raw name", response = String.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "unable to do migration", response = String.class) })
	public Response migrateRawName(@Context HttpServletRequest request) {
		try {
			if(!TaxonomyUtil.isAdmin(request))
				throw new WebApplicationException(
						Response.status(Response.Status.UNAUTHORIZED).entity("Only admin can do migration").build());
			
			Map<String, Object> result = taxonomyRegistry.snapRawNames();
			return Response.status(Status.OK).entity(result).build();

		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

}
