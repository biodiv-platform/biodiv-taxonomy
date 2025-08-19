package com.strandls.taxonomy.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.strandls.authentication_utility.filter.ValidateUser;
import com.strandls.taxonomy.ApiConstants;
import com.strandls.taxonomy.pojo.response.BreadCrumb;
import com.strandls.taxonomy.pojo.response.TaxonRelation;
import com.strandls.taxonomy.pojo.response.TaxonTree;
import com.strandls.taxonomy.service.TaxonomyRegistryService;
import com.strandls.taxonomy.util.TaxonomyUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Tag(name = "Taxonomy Tree Services", description = "APIs for taxonomy tree registry operations")
@Path(ApiConstants.V1 + ApiConstants.TREE)
@Produces(MediaType.APPLICATION_JSON)
public class TaxonomyRegistryController {

	@Inject
	private TaxonomyRegistryService taxonomyRegistry;

	@GET
	@Path(ApiConstants.BREADCRUMB + "/{taxonomyId}")
	@Consumes(MediaType.TEXT_PLAIN)
	@Operation(summary = "Find Taxonomy Registry by ID", responses = {
			@ApiResponse(responseCode = "200", description = "List of breadcrumb items", content = @Content(array = @ArraySchema(schema = @Schema(implementation = BreadCrumb.class)))),
			@ApiResponse(responseCode = "404", description = "Taxonomy not found", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response getTaxonomyBreadCrumb(
			@Parameter(description = "Taxonomy ID", required = true) @PathParam("taxonomyId") String taxonomyId) {
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
	@ValidateUser
	@Operation(summary = "Find taxon tree for a list of taxons", responses = {
			@ApiResponse(responseCode = "200", description = "List of TaxonTree objects", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaxonTree.class)))),
			@ApiResponse(responseCode = "400", description = "Unable to fetch the taxon tree", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response getTaxonTree(@Context HttpServletRequest request,
			@Parameter(description = "Comma-separated list of taxon IDs", example = "123,456,789") @QueryParam("taxonList") String taxonList) {
		try {
			String[] taxList = taxonList.split(",");
			List<Long> tList = new ArrayList<>();
			for (String s : taxList) {
				tList.add(Long.parseLong(s.trim()));
			}
			List<TaxonTree> result = taxonomyRegistry.fetchTaxonTrees(tList);
			return Response.status(Status.OK).entity(result).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	/** Returns a list of taxon relations (dummy method comments removed) */
	@GET
	@Path("/list")
	@Transactional
	@Operation(summary = "Get taxon relationships", responses = {
			@ApiResponse(responseCode = "200", description = "List of taxon relationships", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaxonRelation.class)))),
			@ApiResponse(responseCode = "400", description = "Unable to fetch the taxon relationship", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response list(@Parameter(description = "Parent taxon ID") @QueryParam("parent") Long parent,
			@Parameter(description = "Classification ID") @QueryParam("classification") Long classificationId,
			@Parameter(description = "Comma-separated taxon IDs") @QueryParam("taxonIds") String taxonIds,
			@Parameter(description = "Expand taxon", example = "false") @DefaultValue("false") @QueryParam("expand_taxon") Boolean expandTaxon) {
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
	@ValidateUser
	@Operation(summary = "Migrate the taxonomy hierarchy to the clean list", description = "Migrate hierarchy following specific order", responses = {
			@ApiResponse(responseCode = "200", description = "Migration result", content = @Content(schema = @Schema(implementation = String.class))),
			@ApiResponse(responseCode = "400", description = "Unable to do migration", content = @Content(schema = @Schema(implementation = String.class))),
			@ApiResponse(responseCode = "401", description = "Admin only", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response migrateCleanName(@Context HttpServletRequest request) {
		try {
			if (!TaxonomyUtil.isAdmin(request)) {
				throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
						.entity("Only admin can do clean list migration").build());
			}
			Map<String, Object> result = taxonomyRegistry.migrateCleanName();
			return Response.status(Status.OK).entity(result).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@POST
	@Path("/migrate/working")
	@ValidateUser
	@Operation(summary = "Migrate the taxonomy hierarchy for working name", responses = {
			@ApiResponse(responseCode = "200", description = "Migration result", content = @Content(schema = @Schema(implementation = String.class))),
			@ApiResponse(responseCode = "400", description = "Unable to do migration", content = @Content(schema = @Schema(implementation = String.class))),
			@ApiResponse(responseCode = "401", description = "Admin only", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response migrateWorkingName(@Context HttpServletRequest request) {
		try {
			if (!TaxonomyUtil.isAdmin(request)) {
				throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
						.entity("Only admin can do working list migration").build());
			}
			Map<String, Object> result = taxonomyRegistry.snapWorkingNames();
			return Response.status(Status.OK).entity(result).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@POST
	@Path("/migrate/raw")
	@ValidateUser
	@Operation(summary = "Migrate the taxonomy hierarchy for raw name", responses = {
			@ApiResponse(responseCode = "200", description = "Migration result", content = @Content(schema = @Schema(implementation = String.class))),
			@ApiResponse(responseCode = "400", description = "Unable to do migration", content = @Content(schema = @Schema(implementation = String.class))),
			@ApiResponse(responseCode = "401", description = "Admin only", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response migrateRawName(@Context HttpServletRequest request) {
		try {
			if (!TaxonomyUtil.isAdmin(request)) {
				throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
						.entity("Only admin can do raw list migration").build());
			}
			Map<String, Object> result = taxonomyRegistry.snapRawNames();
			return Response.status(Status.OK).entity(result).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}
}
