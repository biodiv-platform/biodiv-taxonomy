package com.strandls.taxonomy.controller;

import java.util.List;

import com.strandls.authentication_utility.filter.ValidateUser;
import com.strandls.taxonomy.ApiConstants;
import com.strandls.taxonomy.pojo.CommonName;
import com.strandls.taxonomy.pojo.CommonNamesData;
import com.strandls.taxonomy.service.CommonNameSerivce;

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
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Tag(name = "Common Name Services", description = "APIs for managing common names")
@Path(ApiConstants.V1 + ApiConstants.CNAME)
@Produces(MediaType.APPLICATION_JSON)
public class CommonNameController {

	@Inject
	private CommonNameSerivce commonNameService;

	@GET
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "Get the common name", description = "Get a specific common name by ID", responses = {
			@ApiResponse(responseCode = "200", description = "The common name", content = @Content(schema = @Schema(implementation = CommonName.class))),
			@ApiResponse(responseCode = "404", description = "Could not find the common name", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response getCommonName(@Context HttpServletRequest request,
			@Parameter(description = "Common name ID", required = true) @PathParam("id") Long id) {
		try {
			CommonName commonName = commonNameService.fetchById(id);
			return Response.ok().entity(commonName).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@GET
	@Path("/taxon")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "Get all common names for a taxon", description = "Get all common names for a given taxon ID", responses = {
			@ApiResponse(responseCode = "200", description = "All common names for a taxon", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CommonName.class)))),
			@ApiResponse(responseCode = "404", description = "Could not find the common name", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response getCommonNameForTaxonId(@Context HttpServletRequest request,
			@Parameter(description = "Taxon ID") @QueryParam("taxonId") Long taxonId) {
		try {
			List<CommonName> commonNames = commonNameService.fetchByTaxonId(taxonId);
			return Response.ok().entity(commonNames).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@ValidateUser
	@Operation(summary = "Add a common name", description = "Save a new common name", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CommonName.class))), responses = {
			@ApiResponse(responseCode = "200", description = "The new common name", content = @Content(schema = @Schema(implementation = CommonName.class))),
			@ApiResponse(responseCode = "404", description = "Could not add common name", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response save(@Context HttpServletRequest request, CommonName commonName) {
		try {
			commonName = commonNameService.save(commonName);
			return Response.ok().entity(commonName).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@GET
	@Path("preffered")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(summary = "Get the preferred common name for a taxon", description = "Returns the preferred common name for a given taxon ID", responses = {
			@ApiResponse(responseCode = "200", description = "Preferred common name", content = @Content(schema = @Schema(implementation = CommonName.class))),
			@ApiResponse(responseCode = "404", description = "Preferred common name is not set", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response getPrefferedCommanName(@Context HttpServletRequest request,
			@Parameter(description = "Taxon ID") @QueryParam("taxonId") Long taxonId) {
		try {
			CommonName commonName = commonNameService.getPrefferedCommonName(taxonId);
			return Response.ok().entity(commonName).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@PUT
	@Path("preffered")
	@Consumes(MediaType.APPLICATION_JSON)
	@ValidateUser
	@Operation(summary = "Update the preferred common name", description = "Set a common name to be preferred", requestBody = @RequestBody(required = true, description = "Common name ID (as raw long in body)", content = @Content(schema = @Schema(implementation = Long.class))), responses = {
			@ApiResponse(responseCode = "200", description = "Updated preferred common name", content = @Content(schema = @Schema(implementation = CommonName.class))),
			@ApiResponse(responseCode = "404", description = "Could not set the common name to preferred", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response updateIsPreffered(@Context HttpServletRequest request, Long id) {
		try {
			CommonName commonName = commonNameService.updateIsPreffered(id);
			return Response.ok().entity(commonName).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@PUT
	@Path(ApiConstants.UPDATE + ApiConstants.COMMONNAME)
	@Consumes(MediaType.APPLICATION_JSON)
	@ValidateUser
	@Operation(summary = "Update/add common names for a species", description = "Add or update a set of common names for a species", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CommonNamesData.class))), responses = {
			@ApiResponse(responseCode = "200", description = "The new list of common names", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CommonName.class)))),
			@ApiResponse(responseCode = "400", description = "Unable to update the common name", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response updateAddCommonNames(@Context HttpServletRequest request,
			@Parameter(description = "Species ID") @QueryParam("speciesId") String speciesId,
			CommonNamesData commonNamesData) {
		try {
			Long sId = null;
			if (speciesId != null)
				sId = Long.parseLong(speciesId);
			List<CommonName> result = commonNameService.updateAddCommonName(request, sId, commonNamesData);
			return Response.status(Status.OK).entity(result).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	@DELETE
	@Path(ApiConstants.REMOVE + ApiConstants.COMMONNAME + "/{commonNameId}")
	@Consumes(MediaType.TEXT_PLAIN)
	@ValidateUser
	@Operation(summary = "Remove a common name", description = "Remove a common name for a given species, by ID", responses = {
			@ApiResponse(responseCode = "200", description = "Deleted (remaining) common names", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CommonName.class)))),
			@ApiResponse(responseCode = "400", description = "Unable to remove the common name", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response removeCommonName(@Context HttpServletRequest request,
			@Parameter(description = "Species ID") @QueryParam("speciesId") String speciesId,
			@Parameter(description = "Common name ID", required = true) @PathParam("commonNameId") String commonNameId) {
		try {
			Long cnId = Long.parseLong(commonNameId);
			Long sId = null;
			if (speciesId != null)
				sId = Long.parseLong(speciesId);
			List<CommonName> result = commonNameService.removeCommonName(request, sId, cnId);
			return Response.status(Status.OK).entity(result).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}
}
