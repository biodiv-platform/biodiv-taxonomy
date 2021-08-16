package com.strandls.taxonomy.controller;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
import com.strandls.taxonomy.pojo.CommonName;
import com.strandls.taxonomy.pojo.CommonNamesData;
import com.strandls.taxonomy.service.CommonNameSerivce;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api("Common Name Services")
@Path(ApiConstants.V1 + ApiConstants.CNAME)
public class CommonNameController {

	@Inject
	private CommonNameSerivce commonNameService;

	@GET
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)

	@ApiOperation(value = "Get the common name", notes = "Get the common name", response = CommonName.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Could not find the common name", response = String.class) })
	public Response getCommonName(@Context HttpServletRequest request, @PathParam("id") Long id) {
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
	@Produces(MediaType.APPLICATION_JSON)

	@ApiOperation(value = "Get the common name", notes = "Get the common name", response = CommonName.class, responseContainer = "List")
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Could not find the common name", response = String.class) })
	public Response getCommonNameForTaxonId(@Context HttpServletRequest request, @QueryParam("taxonId") Long taxonId) {
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
	@Produces(MediaType.APPLICATION_JSON)

	@ValidateUser

	@ApiOperation(value = "Add the common name", notes = "Save the common name", response = CommonName.class)
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Could not add common name", response = String.class) })
	public Response save(@Context HttpServletRequest request, @ApiParam("commonName") CommonName commonName) {
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
	@Produces(MediaType.APPLICATION_JSON)

	@ApiOperation(value = "Get the preffered common name over all for given taxon id", notes = "Return the common name", response = CommonName.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Preffered common name is not set", response = String.class) })
	public Response getPrefferedCommanName(@Context HttpServletRequest request, @QueryParam("taxonId") Long taxonId) {
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
	@Produces(MediaType.APPLICATION_JSON)

	@ValidateUser

	@ApiOperation(value = "Update the preffered common name over all", notes = "Return the common name", response = CommonName.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Could not set the common name to preffered", response = String.class) })
	public Response updateIsPreffered(@Context HttpServletRequest request, @QueryParam("commonNameId") Long id) {
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
	@Produces(MediaType.APPLICATION_JSON)

	@ValidateUser

	@ApiOperation(value = "update add the commonName", notes = "Returns the new list of commonName", response = CommonName.class, responseContainer = "List")
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "unable to update the commonName", response = String.class) })

	public Response updateAddCommonNames(@Context HttpServletRequest request, @QueryParam("speciesId") String speciesId,
			@ApiParam(name = "commonNameData") CommonNamesData commonNamesData) {
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
	@Produces(MediaType.APPLICATION_JSON)

	@ValidateUser

	@ApiOperation(value = "remove the commonName", notes = "Returns the Boolean values", response = CommonName.class, responseContainer = "List")
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "unable to remove the commonName", response = String.class) })

	public Response removeCommonName(@Context HttpServletRequest request, @QueryParam("speciesId") String speciesId,
			@ApiParam(name = "commonNameId") @PathParam("commonNameId") String commonNameId) {
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
