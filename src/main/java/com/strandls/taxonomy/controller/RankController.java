package com.strandls.taxonomy.controller;

import java.util.List;

import com.strandls.authentication_utility.filter.ValidateUser;
import com.strandls.taxonomy.ApiConstants;
import com.strandls.taxonomy.pojo.Rank;
import com.strandls.taxonomy.service.RankSerivce;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Tag(name = "Rank Services", description = "APIs for managing taxonomic ranks")
@Path(ApiConstants.V1 + ApiConstants.RANK)
@Produces(MediaType.APPLICATION_JSON)
public class RankController {

	@Inject
	private RankSerivce rankService;

	@GET
	@Path("all")
	@Operation(summary = "Get all the ranks", responses = {
			@ApiResponse(responseCode = "200", description = "List of all ranks", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Rank.class)))),
			@ApiResponse(responseCode = "404", description = "Ranks not found", content = @Content(schema = @Schema(implementation = String.class))) })
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
	@ValidateUser
	@Operation(summary = "Add required rank for the taxonomy tree", responses = {
			@ApiResponse(responseCode = "200", description = "Added or updated required rank", content = @Content(schema = @Schema(implementation = Rank.class))),
			@ApiResponse(responseCode = "404", description = "Could not add the rank", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response addRank(@Context HttpServletRequest request,
			@Parameter(description = "Name of the required rank", required = true) @PathParam("rankName") String rankName,
			@Parameter(description = "Value of the required rank", required = true) @PathParam("rankValue") Double rankValue) {
		try {
			Rank rank = rankService.addRequiredRank(request, rankName, rankValue);
			return Response.status(Status.OK).entity(rank).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).build();
		}
	}

	@POST
	@Path("/intermediate/{rankName}/{highRankName}/{lowRankName}")
	@ValidateUser
	@Operation(summary = "Add intermediate rank for the taxonomy tree", responses = {
			@ApiResponse(responseCode = "200", description = "Added or updated intermediate rank", content = @Content(schema = @Schema(implementation = Rank.class))),
			@ApiResponse(responseCode = "404", description = "Could not add the rank", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response addRank(@Context HttpServletRequest request,
			@Parameter(description = "Name of the new rank", required = true) @PathParam("rankName") String rankName,
			@Parameter(description = "Higher rank name", required = true) @PathParam("highRankName") String highRankName,
			@Parameter(description = "Lower rank name", required = true) @PathParam("lowRankName") String lowRankName) {
		try {
			Rank rank = rankService.addIntermediateRank(request, rankName, highRankName, lowRankName);
			return Response.status(Status.OK).entity(rank).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).build();
		}
	}
}
