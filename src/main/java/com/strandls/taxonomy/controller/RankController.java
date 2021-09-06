package com.strandls.taxonomy.controller;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
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
import com.strandls.taxonomy.pojo.Rank;
import com.strandls.taxonomy.pojo.TaxonomyDefinition;
import com.strandls.taxonomy.service.RankSerivce;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api("Rank Services")
@Path(ApiConstants.V1 + ApiConstants.RANK)
public class RankController {

	@Inject
	private RankSerivce rankService;

	@GET
	@Path("all")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get all the ranks", notes = "Get all the ranks", response = TaxonomyDefinition.class)
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Could not add the ranks", response = String.class) })
	public Response getAllRank(@Context HttpServletRequest request) {
		try {
			List<Rank> ranks = rankService.getAllRank(request);
			return Response.status(Status.OK).entity(ranks).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).build();
		}
	}
	
	@POST
	@Path("/required/{rankName}/{rankValue}")
	@Produces(MediaType.APPLICATION_JSON)
	@ValidateUser
	@ApiOperation(value = "Add rank for the taxonomy tree", notes = "Add rank for the tree", response = TaxonomyDefinition.class)
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Could not add the ranks", response = String.class) })
	public Response addRank(@Context HttpServletRequest request, @PathParam("rankName") String rankName,
			@PathParam("rankValue") Double rankValue) {
		try {
			Rank rank = rankService.addRequiredRank(request, rankName, rankValue);
			return Response.status(Status.OK).entity(rank).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).build();
		}
	}
	
	@POST
	@Path("/intermediate/{rankName}/{highRankName}/{lowRankName}")
	@Produces(MediaType.APPLICATION_JSON)
	@ValidateUser
	@ApiOperation(value = "Add rank for the taxonomy tree", notes = "Add rank for the tree", response = TaxonomyDefinition.class)
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Could not add the ranks", response = String.class) })
	public Response addRank(@Context HttpServletRequest request, @PathParam("rankName") String rankName,
			@PathParam("highRankName") String highRankName, @PathParam("lowRankName") String lowRankName) {
		try {
			Rank rank = rankService.addIntermediateRank(request, rankName, highRankName, lowRankName);
			return Response.status(Status.OK).entity(rank).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).build();
		}
	}
}
