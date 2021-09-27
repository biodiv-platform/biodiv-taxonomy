/**
 * 
 */
package com.strandls.taxonomy.controller;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.strandls.authentication_utility.filter.ValidateUser;
import com.strandls.taxonomy.ApiConstants;
import com.strandls.taxonomy.pojo.EncryptedKey;
import com.strandls.taxonomy.pojo.PermissionData;
import com.strandls.taxonomy.service.TaxonomyPermisisonService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * @author Abhishek Rudra
 *
 * 
 */

@Api("Taxonomy Permission Service")
@Path(ApiConstants.V1 + ApiConstants.PERMISSION)
public class TaxonomyPermissionController {

	@Inject
	private TaxonomyPermisisonService permissionService;

	@GET
	@Path(ApiConstants.SPECIES + "/{taxonId}")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)

	@ValidateUser

	@ApiOperation(value = "check the permission on taxon tree for speciesContributor and taxonomy contributor role", notes = "Return boolean value", response = Boolean.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "unable to check the permission", response = String.class) })

	public Response getPermissionSpeciesTree(@Context HttpServletRequest request,
			@PathParam("taxonId") String taxonId) {
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
	@Produces(MediaType.APPLICATION_JSON)

	@ValidateUser

	@ApiOperation(value = "check the permission on taxon tree for observation Curator role", notes = "Return boolean value", response = Boolean.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "unable to check the permission", response = String.class) })

	public Response isObservationCurator(@Context HttpServletRequest request, @PathParam("taxonId") String taxonId) {
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
	@Produces(MediaType.APPLICATION_JSON)

	@ValidateUser
	@ApiOperation(value = "assign permission directly", notes = "Responds Boolean value", response = Boolean.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "unable to assign the permission", response = String.class) })

	public Response assignDirectPermission(@Context HttpServletRequest request,
			@ApiParam(name = "permissionData") PermissionData permissionData) {
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
	@Produces(MediaType.APPLICATION_JSON)

	@ValidateUser
	@ApiOperation(value = "Send request for permission over a taxonomyNode", notes = "sends mail to the permission", response = Boolean.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "unable to send the req", response = String.class) })

	public Response requestPermission(@Context HttpServletRequest request,
			@ApiParam(name = "permissionData") PermissionData permissionData) {
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
	@Produces(MediaType.APPLICATION_JSON)

	@ValidateUser

	@ApiOperation(value = "validate the request for permission over a taxonomyId", notes = "checks the grants the permission", response = Boolean.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "uable to grant the permission", response = String.class) })

	public Response grantPermissionrequest(@Context HttpServletRequest request,
			@ApiParam(name = "encryptedKey") EncryptedKey encryptedKey) {
		try {
			Boolean result = permissionService.verifyPermissionGrant(request, encryptedKey);
			if (result.booleanValue())
				return Response.status(Status.OK).entity(result).build();
			return Response.status(Status.NOT_IMPLEMENTED).build();

		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

}
