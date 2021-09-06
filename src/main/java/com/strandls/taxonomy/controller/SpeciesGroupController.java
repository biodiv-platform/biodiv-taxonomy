package com.strandls.taxonomy.controller;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.pac4j.core.profile.CommonProfile;

import com.strandls.authentication_utility.filter.ValidateUser;
import com.strandls.authentication_utility.util.AuthUtil;
import com.strandls.taxonomy.ApiConstants;
import com.strandls.taxonomy.pojo.SpeciesGroup;
import com.strandls.taxonomy.pojo.SpeciesGroupMapping;
import com.strandls.taxonomy.pojo.SpeciesPermission;
import com.strandls.taxonomy.service.SpeciesGroupService;
import com.strandls.taxonomy.util.TaxonomyUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api("Species Services")
@Path(ApiConstants.V1 + ApiConstants.SPECIES)
public class SpeciesGroupController {
	
	@Inject
	private SpeciesGroupService speciesGroupService;
	
	@GET
	@Path("taxon")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)

	@ApiOperation(value = "Get species group from the given taxon id", notes = "Returns Group details", response = SpeciesGroup.class)
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Taxonomy not found", response = String.class) })

	public Response getGroupId(@QueryParam("taxonId") Long taxonId) {
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
	@Produces(MediaType.APPLICATION_JSON)
	
	@ValidateUser
	
	@ApiOperation(value = "Add species group", notes = "Return added species group", response = SpeciesGroup.class)
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Could not add species group", response = String.class) })
	
	public Response addSpeciesGroup(@Context HttpServletRequest request, @ApiParam("speciesGroup") SpeciesGroup speciesGroup) {
		try {
			if(!TaxonomyUtil.isAdmin(request))
				return Response.status(Status.UNAUTHORIZED).entity("Only admin can add the species group").build();
			speciesGroup = speciesGroupService.save(speciesGroup);
			return Response.status(Status.OK).entity(speciesGroup).build();
		} catch(Exception e) {
			return Response.status(Status.BAD_REQUEST).build();
		}
	}
	
	@POST
	@Path(ApiConstants.MAPPING)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	
	@ValidateUser
	
	@ApiOperation(value = "Add species group mapping", notes = "Return a added species group mapping", response = SpeciesGroup.class)
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Could not add species group mapping", response = String.class) })
	
	public Response addSpeciesGroupMapping(@Context HttpServletRequest request, @ApiParam("speciesGroupMapping") SpeciesGroupMapping speciesGroupMapping) {
		try {
			speciesGroupMapping = speciesGroupService.save(speciesGroupMapping);
			return Response.status(Status.OK).entity(speciesGroupMapping).build();
		} catch(Exception e) {
			return Response.status(Status.BAD_REQUEST).build();
		}
	}
	
	@POST
	@Path(ApiConstants.PERMISSION)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	
	@ValidateUser
	
	@ApiOperation(value = "Add species group mapping", notes = "Return a added species group mapping", response = SpeciesGroup.class)
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Could not add species group mapping", response = String.class) })
	
	public Response addSpeciesPermission(@Context HttpServletRequest request, @ApiParam("speciesPermission") SpeciesPermission speciesPermission) {
		try {
			speciesPermission = speciesGroupService.save(speciesPermission);
			return Response.status(Status.OK).entity(speciesPermission).build();
		} catch(Exception e) {
			return Response.status(Status.BAD_REQUEST).build();
		}
	}
	
	@GET
	@Path("/{speciesGroupId}")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)

	@ApiOperation(value = "Find taxonomy by SpeciesId", notes = "Return a List of Species Id", response = String.class, responseContainer = "List")
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Taxonomy not Found", response = String.class) })

	public Response getTaxonomyBySpeciesGroup(@PathParam("speciesGroupId") String sGroup,
			@QueryParam("taxonomyList") List<String> taxonList) {
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
	@Produces(MediaType.APPLICATION_JSON)

	@ApiOperation(value = "Find all the SpeciesGroup", notes = "Returns all speciesGroup", response = SpeciesGroup.class, responseContainer = "List")
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Species Group not Found", response = String.class) })

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
	@Produces(MediaType.APPLICATION_JSON)

	@ValidateUser

	@ApiOperation(value = "get the speciesPermisison", notes = "return list of taxonomy id in which user can validate", response = SpeciesPermission.class, responseContainer = "List")
	@ApiResponses(value = { @ApiResponse(code = 400, message = "unable to get the list", response = String.class) })

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
