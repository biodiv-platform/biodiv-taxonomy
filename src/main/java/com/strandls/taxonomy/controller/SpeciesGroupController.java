package com.strandls.taxonomy.controller;

import java.util.List;

import org.pac4j.core.profile.CommonProfile;

import com.strandls.authentication_utility.filter.ValidateUser;
import com.strandls.authentication_utility.util.AuthUtil;
import com.strandls.taxonomy.ApiConstants;
import com.strandls.taxonomy.pojo.SpeciesGroup;
import com.strandls.taxonomy.pojo.SpeciesGroupMapping;
import com.strandls.taxonomy.pojo.SpeciesPermission;
import com.strandls.taxonomy.service.SpeciesGroupService;
import com.strandls.taxonomy.util.TaxonomyUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Tag(name = "Species Services", description = "APIs for managing species groups and permissions")
@Path(ApiConstants.V1 + ApiConstants.SPECIES)
@Produces(MediaType.APPLICATION_JSON)
public class SpeciesGroupController {

	@Inject
	private SpeciesGroupService speciesGroupService;

	@GET
	@Path("taxon")
	@Consumes(MediaType.TEXT_PLAIN)
	@Operation(summary = "Get species group from a given taxon id", responses = {
			@ApiResponse(responseCode = "200", description = "Group details", content = @Content(schema = @Schema(implementation = SpeciesGroup.class))),
			@ApiResponse(responseCode = "404", description = "Taxonomy not found", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response getGroupId(
			@Parameter(description = "Taxon ID", required = true) @QueryParam("taxonId") Long taxonId) {
		try {
			SpeciesGroup speciesGroup = speciesGroupService.getGroupByTaxonId(taxonId);
			return Response.status(Status.OK).entity(speciesGroup).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).build();
		}
	}

	@POST
	@Path(ApiConstants.GROUP)
	@Consumes(MediaType.APPLICATION_JSON)
	@ValidateUser
	@Operation(summary = "Add species group", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = SpeciesGroup.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Added species group", content = @Content(schema = @Schema(implementation = SpeciesGroup.class))),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = String.class))),
			@ApiResponse(responseCode = "404", description = "Could not add species group", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response addSpeciesGroup(@Context HttpServletRequest request, SpeciesGroup speciesGroup) {
		try {
			if (!TaxonomyUtil.isAdmin(request))
				return Response.status(Status.UNAUTHORIZED).entity("Only admin can add the species group").build();
			speciesGroup = speciesGroupService.save(speciesGroup);
			return Response.status(Status.OK).entity(speciesGroup).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).build();
		}
	}

	@POST
	@Path(ApiConstants.MAPPING)
	@Consumes(MediaType.APPLICATION_JSON)
	@ValidateUser
	@Operation(summary = "Add species group mapping", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = SpeciesGroupMapping.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Added species group mapping", content = @Content(schema = @Schema(implementation = SpeciesGroupMapping.class))),
			@ApiResponse(responseCode = "404", description = "Could not add species group mapping", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response addSpeciesGroupMapping(@Context HttpServletRequest request,
			SpeciesGroupMapping speciesGroupMapping) {
		try {
			speciesGroupMapping = speciesGroupService.save(speciesGroupMapping);
			return Response.status(Status.OK).entity(speciesGroupMapping).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).build();
		}
	}

	@POST
	@Path(ApiConstants.PERMISSION)
	@Consumes(MediaType.APPLICATION_JSON)
	@ValidateUser
	@Operation(summary = "Add species permission mapping", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = SpeciesPermission.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Added species group permission", content = @Content(schema = @Schema(implementation = SpeciesPermission.class))),
			@ApiResponse(responseCode = "404", description = "Could not add species group mapping", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response addSpeciesPermission(@Context HttpServletRequest request, SpeciesPermission speciesPermission) {
		try {
			speciesPermission = speciesGroupService.save(speciesPermission);
			return Response.status(Status.OK).entity(speciesPermission).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).build();
		}
	}

	@GET
	@Path("/{speciesGroupId}")
	@Consumes(MediaType.TEXT_PLAIN)
	@Operation(summary = "Find taxonomy by SpeciesGroup ID", responses = {
			@ApiResponse(responseCode = "200", description = "List of taxonomy IDs", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))),
			@ApiResponse(responseCode = "404", description = "Taxonomy not found", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response getTaxonomyBySpeciesGroup(
			@Parameter(description = "Species Group ID", required = true) @PathParam("speciesGroupId") String sGroup,
			@Parameter(description = "Optional taxonomy filter list") @QueryParam("taxonomyList") List<String> taxonList) {
		try {
			Long speciesId = Long.parseLong(sGroup);
			List<String> taxonomyList = speciesGroupService.fetchBySpeciesGroupId(speciesId, taxonList);
			return Response.status(Status.OK).entity(taxonomyList).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).build();
		}
	}

	@GET
	@Path(ApiConstants.ALL)
	@Operation(summary = "Find all the SpeciesGroup", responses = {
			@ApiResponse(responseCode = "200", description = "All species groups", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SpeciesGroup.class)))),
			@ApiResponse(responseCode = "404", description = "Species Group not found", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response getAllSpeciesGroup() {
		try {
			List<SpeciesGroup> result = speciesGroupService.findAllSpecies();
			return Response.status(Status.OK).entity(result).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).build();
		}
	}

	@GET
	@Path(ApiConstants.PERMISSION)
	@ValidateUser
	@Operation(summary = "Get the species permission", responses = {
			@ApiResponse(responseCode = "200", description = "List of taxonomy IDs the user can validate", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SpeciesPermission.class)))),
			@ApiResponse(responseCode = "400", description = "Unable to get the permission list", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response getSpeciesPermission(@Context HttpServletRequest request) {
		try {
			CommonProfile profile = AuthUtil.getProfileFromRequest(request);
			Long userId = Long.parseLong(profile.getId());
			List<SpeciesPermission> result = speciesGroupService.getSpeciesPermissions(userId);
			return Response.status(Status.OK).entity(result).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}
}
