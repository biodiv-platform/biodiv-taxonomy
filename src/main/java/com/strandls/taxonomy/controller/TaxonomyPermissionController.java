package com.strandls.taxonomy.controller;

import com.strandls.authentication_utility.filter.ValidateUser;
import com.strandls.taxonomy.ApiConstants;
import com.strandls.taxonomy.pojo.EncryptedKey;
import com.strandls.taxonomy.pojo.PermissionData;
import com.strandls.taxonomy.service.TaxonomyPermisisonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Tag(name = "Taxonomy Permission Service", description = "APIs for taxonomy permission microservice")
@Path(ApiConstants.V1 + ApiConstants.PERMISSION)
@Produces(MediaType.APPLICATION_JSON)
public class TaxonomyPermissionController {

	@Inject
	private TaxonomyPermisisonService permissionService;

	@GET
	@Path(ApiConstants.PING)
	@Produces(MediaType.TEXT_PLAIN)
	@Operation(summary = "Dummy API Ping", description = "Checks validity of war file at deployment", responses = @ApiResponse(responseCode = "200", description = "PONG", content = @Content(schema = @Schema(implementation = String.class))))
	public Response ping() {
		return Response.status(Status.OK).entity("PONG").build();
	}

	@GET
	@Path(ApiConstants.SPECIES + "/{taxonId}")
	@Consumes(MediaType.TEXT_PLAIN)
	@ValidateUser
	@Operation(summary = "Check permission for speciesContributor and taxonomy contributor role", description = "Return boolean value for permission on taxon tree", responses = {
			@ApiResponse(responseCode = "200", description = "Permission check result", content = @Content(schema = @Schema(implementation = Boolean.class))),
			@ApiResponse(responseCode = "400", description = "Unable to check the permission", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response getPermissionSpeciesTree(@Context HttpServletRequest request,
			@Parameter(description = "Taxonomy ID", required = true) @PathParam("taxonId") String taxonId) {
		try {
			Long taxonomyId = Long.parseLong(taxonId);
			Boolean result = permissionService.getPermissionOnTree(request, taxonomyId);
			return Response.status(Status.OK).entity(result).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	@GET
	@Path(ApiConstants.OBSERVATION + "/{taxonId}")
	@Consumes(MediaType.TEXT_PLAIN)
	@ValidateUser
	@Operation(summary = "Check permission for observation Curator role", description = "Return boolean value for permission on taxon tree", responses = {
			@ApiResponse(responseCode = "200", description = "Permission check result", content = @Content(schema = @Schema(implementation = Boolean.class))),
			@ApiResponse(responseCode = "400", description = "Unable to check the permission", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response isObservationCurator(@Context HttpServletRequest request,
			@Parameter(description = "Taxonomy ID", required = true) @PathParam("taxonId") String taxonId) {
		try {
			Long taxonomyId = Long.parseLong(taxonId);
			Boolean result = permissionService.checkIsObservationCurator(request, taxonomyId);
			return Response.status(Status.OK).entity(result).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	@POST
	@Path(ApiConstants.ASSIGN)
	@Consumes(MediaType.APPLICATION_JSON)
	@ValidateUser
	@Operation(summary = "Assign permission directly", description = "Directly assign permission to a user", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = PermissionData.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Assignment success (true/false)", content = @Content(schema = @Schema(implementation = Boolean.class))),
			@ApiResponse(responseCode = "405", description = "Assignment not allowed", content = @Content(schema = @Schema(implementation = String.class))),
			@ApiResponse(responseCode = "400", description = "Unable to assign the permission", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response assignDirectPermission(@Context HttpServletRequest request, PermissionData permissionData) {
		try {
			Boolean result = permissionService.assignUpdatePermissionDirectly(request, permissionData);
			if (result != null && result)
				return Response.status(Status.OK).entity(result).build();
			return Response.status(Status.METHOD_NOT_ALLOWED).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	@POST
	@Path(ApiConstants.REQUEST)
	@Consumes(MediaType.APPLICATION_JSON)
	@ValidateUser
	@Operation(summary = "Send request for permission over a taxonomy node", description = "Sends mail to the permission", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = PermissionData.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Permission request sent", content = @Content(schema = @Schema(implementation = Boolean.class))),
			@ApiResponse(responseCode = "304", description = "Not modified", content = @Content(schema = @Schema(implementation = String.class))),
			@ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = String.class))),
			@ApiResponse(responseCode = "400", description = "Unable to send the request", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response requestPermission(@Context HttpServletRequest request, PermissionData permissionData) {
		try {
			Boolean result = permissionService.requestPermission(request, permissionData);
			if (result != null) {
				if (result)
					return Response.status(Status.OK).entity(result).build();
				return Response.status(Status.NOT_MODIFIED).build();
			}
			return Response.status(Status.NOT_FOUND).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	@POST
	@Path(ApiConstants.GRANT)
	@Consumes(MediaType.APPLICATION_JSON)
	@ValidateUser
	@Operation(summary = "Validate the request for permission over a taxonomyId", description = "Checks and grants the permission", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = EncryptedKey.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Permission grant successful", content = @Content(schema = @Schema(implementation = Boolean.class))),
			@ApiResponse(responseCode = "501", description = "Not implemented", content = @Content(schema = @Schema(implementation = String.class))),
			@ApiResponse(responseCode = "400", description = "Unable to grant the permission", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response grantPermissionrequest(@Context HttpServletRequest request, EncryptedKey encryptedKey) {
		try {
			Boolean result = permissionService.verifyPermissionGrant(request, encryptedKey);
			if (result != null && result)
				return Response.status(Status.OK).entity(result).build();
			return Response.status(Status.NOT_IMPLEMENTED).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}
}
