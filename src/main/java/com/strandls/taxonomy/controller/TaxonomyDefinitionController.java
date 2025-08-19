package com.strandls.taxonomy.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import com.strandls.activity.pojo.Activity;
import com.strandls.activity.pojo.CommentLoggingData;
import com.strandls.authentication_utility.filter.ValidateUser;
import com.strandls.esmodule.pojo.MapQueryResponse;
import com.strandls.taxonomy.ApiConstants;
import com.strandls.taxonomy.dao.TaxonomyDefinitionDao;
import com.strandls.taxonomy.pojo.SynonymData;
import com.strandls.taxonomy.pojo.TaxonomicNames;
import com.strandls.taxonomy.pojo.TaxonomyDefinition;
import com.strandls.taxonomy.pojo.request.FileMetadata;
import com.strandls.taxonomy.pojo.request.TaxonomyPositionUpdate;
import com.strandls.taxonomy.pojo.request.TaxonomySave;
import com.strandls.taxonomy.pojo.request.TaxonomyStatusUpdate;
import com.strandls.taxonomy.pojo.response.TaxonomyDefinitionShow;
import com.strandls.taxonomy.pojo.response.TaxonomyNameListResponse;
import com.strandls.taxonomy.pojo.response.TaxonomySearch;
import com.strandls.taxonomy.service.TaxonomyDefinitionSerivce;
import com.strandls.taxonomy.service.impl.TaxonomyESOperation;
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
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
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

@Tag(name = "Taxonomy Services", description = "APIs for taxonomy microservice")
@Path(ApiConstants.V1 + ApiConstants.TAXONOMY)
@Produces(MediaType.APPLICATION_JSON)
public class TaxonomyDefinitionController {

	@Inject
	private TaxonomyDefinitionSerivce taxonomyService;

	@Inject
	private TaxonomyDefinitionDao taxonomyDefinitionDao;

	@Inject
	private TaxonomyESOperation taxonomyESOperation;

	@GET
	@Path("/{taxonomyConceptId}")
	@Consumes(MediaType.TEXT_PLAIN)
	@Operation(summary = "Find Taxonomy by ID", responses = {
			@ApiResponse(responseCode = "200", description = "Taxonomy details", content = @Content(schema = @Schema(implementation = TaxonomyDefinition.class))),
			@ApiResponse(responseCode = "404", description = "Taxonomy not found", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response getTaxonomyConceptName(
			@Parameter(description = "Taxonomy Concept ID", required = true) @PathParam("taxonomyConceptId") String taxonomyConceptId) {
		try {
			Long id = Long.parseLong(taxonomyConceptId);
			TaxonomyDefinition taxonomy = taxonomyService.fetchById(id);
			return Response.status(Status.OK).entity(taxonomy).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).build();
		}
	}

	@GET
	@Path("/show/{taxonId}")
	@Consumes(MediaType.TEXT_PLAIN)
	@Operation(summary = "Find Taxonomy Details by ID", responses = {
			@ApiResponse(responseCode = "200", description = "Taxonomy details", content = @Content(schema = @Schema(implementation = TaxonomyDefinitionShow.class))),
			@ApiResponse(responseCode = "404", description = "Taxonomy not found", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response getTaxonomyDetails(
			@Parameter(description = "Taxon ID", required = true) @PathParam("taxonId") String taxonId) {
		try {
			Long id = Long.parseLong(taxonId);
			TaxonomyDefinitionShow taxonomyDefinitionShow = taxonomyService.getTaxonomyDetails(id);
			return Response.status(Status.OK).entity(taxonomyDefinitionShow).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).build();
		}
	}

	@GET
	@Path("/namelist")
	@Consumes(MediaType.TEXT_PLAIN)
	@Operation(summary = "Get taxonomy name list", responses = {
			@ApiResponse(responseCode = "200", description = "Taxonomy Name List", content = @Content(schema = @Schema(implementation = TaxonomyNameListResponse.class))),
			@ApiResponse(responseCode = "404", description = "Taxonomy list not found", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response getTaxonomyNameList(@Context HttpServletRequest request,
			@Parameter(description = "Taxon ID") @QueryParam("taxonId") Long taxonId,
			@Parameter(description = "Classification ID") @QueryParam("classificationId") Long classificationId,
			@Parameter(description = "Comma separated rank list") @QueryParam("rankList") String rankList,
			@Parameter(description = "Status list") @QueryParam("statusList") String statusList,
			@Parameter(description = "Position list") @QueryParam("positionList") String positionList,
			@Parameter(description = "Limit", example = "-1") @DefaultValue("-1") @QueryParam("limit") Integer limit,
			@Parameter(description = "Offset", example = "-1") @DefaultValue("-1") @QueryParam("offset") Integer offset) {
		try {
			TaxonomyNameListResponse response = taxonomyService.getTaxonomyNameList(taxonId, classificationId, rankList,
					statusList, positionList, limit, offset);
			return Response.ok().entity(response).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@POST
	@Path("upload")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Operation(summary = "Upload the file for taxon definition", requestBody = @RequestBody(required = true, description = "Multi-part form data with file and associated info"), responses = {
			@ApiResponse(responseCode = "200", description = "Success/failure", content = @Content(schema = @Schema(implementation = FileMetadata.class))),
			@ApiResponse(responseCode = "400", description = "file not present", content = @Content(schema = @Schema(implementation = String.class))),
			@ApiResponse(responseCode = "500", description = "ERROR", content = @Content(schema = @Schema(implementation = String.class))) })
	@ValidateUser
	public Response upload(@Context HttpServletRequest request, final FormDataMultiPart multiPart) {
		try {
			Map<String, Object> result = taxonomyService.uploadFile(request, multiPart);
			return Response.ok().entity(result).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@POST
	@Path(ApiConstants.UPLOAD + ApiConstants.SEARCH)
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Operation(summary = "Upload the file for taxon definition search", requestBody = @RequestBody(required = true, description = "Multi-part file for name matching"), responses = {
			@ApiResponse(responseCode = "200", description = "Success/failure as a map", content = @Content(schema = @Schema(implementation = Map.class))),
			@ApiResponse(responseCode = "400", description = "file not present", content = @Content(schema = @Schema(implementation = String.class))),
			@ApiResponse(responseCode = "500", description = "ERROR", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response uploadSearch(final FormDataMultiPart multiPart) {
		FormDataBodyPart filePart = multiPart.getField("file");
		Integer index = Integer.valueOf(multiPart.getField("column").getValue());
		if (filePart == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("File not present").build();
		}
		try {
			Map<String, Object> result = taxonomyService.nameMatching(filePart, index);
			return Response.ok().entity(result).build();
		} catch (IOException e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	@POST
	@Path("list")
	@Produces(MediaType.APPLICATION_JSON)
	@ValidateUser
	@Operation(summary = "Save the taxonomy list", requestBody = @RequestBody(required = true, content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaxonomySave.class)))), responses = @ApiResponse(responseCode = "200", description = "Saved list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaxonomyDefinition.class)))))
	public Response saveTaxonomyList(@Context HttpServletRequest request, List<TaxonomySave> taxonomyList) {
		try {
			List<TaxonomyDefinition> taxonomyDefinition = taxonomyService.saveList(request, taxonomyList);
			return Response.status(Status.OK).entity(taxonomyDefinition).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@ValidateUser
	@Operation(summary = "Save the taxonomy", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = TaxonomySave.class))), responses = @ApiResponse(responseCode = "200", description = "Saved Taxonomy", content = @Content(schema = @Schema(implementation = TaxonomyDefinition.class))))
	public Response saveTaxonomy(@Context HttpServletRequest request, TaxonomySave taxonomySave) {
		try {
			TaxonomyDefinition taxonomyDefinition = taxonomyService.save(request, taxonomySave);
			return Response.status(Status.OK).entity(taxonomyDefinition).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	@GET
	@Path("/nameSearch")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "Get taxonomy based on the canonical name and rank", responses = @ApiResponse(responseCode = "200", description = "Found taxonomy", content = @Content(schema = @Schema(implementation = TaxonomySearch.class))))
	public Response getByNameSearch(
			@Parameter(description = "Scientific Name") @QueryParam("scientificName") String scientificName,
			@Parameter(description = "Rank name") @QueryParam("rankName") String rankName) {
		try {
			TaxonomySearch taxonomySearch = taxonomyService.getByNameSearch(scientificName, rankName);
			return Response.status(Status.OK).entity(taxonomySearch).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "Search taxonomy based on the name", responses = @ApiResponse(responseCode = "200", description = "Found taxonomy search result", content = @Content(schema = @Schema(implementation = Object.class))))
	public Response search(@Parameter(description = "Search term") @QueryParam("term") String term) {
		try {
			Object name = taxonomyDefinitionDao.search(term);
			return Response.status(Status.OK).entity(name).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@GET
	@Path("/retrieve/specificSearch")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@Operation(summary = "Search taxonomy based on the Ids", responses = @ApiResponse(responseCode = "200", description = "Found taxonomy", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))))
	public Response specificSearch(@Parameter(description = "Search term") @QueryParam("term") String term,
			@Parameter(description = "Classification ID") @QueryParam("classification") Long classificationId,
			@Parameter(description = "Taxon ID") @QueryParam("taxonid") Long taxonid) {
		try {
			List<String> resultTaxonIds = taxonomyDefinitionDao.specificSearch(term, taxonid);
			return Response.status(Status.OK).entity(resultTaxonIds).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@GET
	@Path(ApiConstants.NAMES + "/{taxonomyId}")
	@Consumes(MediaType.TEXT_PLAIN)
	@Operation(summary = "Get the common name and synonyms", responses = {
			@ApiResponse(responseCode = "200", description = "Taxonomic names based on taxonomyId", content = @Content(schema = @Schema(implementation = TaxonomicNames.class))),
			@ApiResponse(responseCode = "400", description = "unable to get the names", content = @Content(schema = @Schema(implementation = String.class))) })
	public Response getNames(
			@Parameter(description = "Taxonomy ID", required = true) @PathParam("taxonomyId") String taxonomyId) {
		try {
			Long taxonId = Long.parseLong(taxonomyId);
			TaxonomicNames result = taxonomyService.findSynonymCommonName(taxonId);
			return Response.status(Status.OK).entity(result).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_GATEWAY).entity(e.getMessage()).build();
		}
	}

	@PUT
	@Path("name")
	@Consumes(MediaType.TEXT_PLAIN)
	@Operation(summary = "Update the name of taxonomy", requestBody = @RequestBody(required = true, description = "The new scientific name"), responses = @ApiResponse(responseCode = "200", description = "Updated taxonomy", content = @Content(schema = @Schema(implementation = TaxonomyDefinitionShow.class))))
	@ValidateUser
	public Response updateName(@Context HttpServletRequest request,
			@Parameter(description = "Taxon ID") @QueryParam("taxonId") Long taxonId,
			@Parameter(description = "Taxon Name") @QueryParam("taxonName") String taxonName) {
		try {
			TaxonomyDefinitionShow taxonomyDefinitionShow = taxonomyService.updateName(request, taxonId, taxonName);
			return Response.status(Status.OK).entity(taxonomyDefinitionShow).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@PUT
	@Path("status")
	@Operation(summary = "Update the taxonomy status (accepted or synonym)", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = TaxonomyStatusUpdate.class))), responses = @ApiResponse(responseCode = "200", description = "Updated taxonomy status", content = @Content(schema = @Schema(implementation = TaxonomyDefinitionShow.class))))
	@ValidateUser
	public Response updateStatus(@Context HttpServletRequest request, TaxonomyStatusUpdate taxonomyStatusUpdate) {
		try {
			TaxonomyDefinitionShow taxonomyDefinitionShow = taxonomyService.updateStatus(request, taxonomyStatusUpdate);
			return Response.status(Status.OK).entity(taxonomyDefinitionShow).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@PUT
	@Path("position")
	@Operation(summary = "Update the position of taxonomy (raw or working)", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = TaxonomyPositionUpdate.class))), responses = @ApiResponse(responseCode = "200", description = "Updated taxonomy position", content = @Content(schema = @Schema(implementation = TaxonomyDefinitionShow.class))))
	@ValidateUser
	public Response updatePosition(@Context HttpServletRequest request, TaxonomyPositionUpdate taxonomyPositionUpdate) {
		try {
			TaxonomyDefinitionShow taxonomyDefinitionShow = taxonomyService.updatePosition(request,
					taxonomyPositionUpdate);
			return Response.status(Status.OK).entity(taxonomyDefinitionShow).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@POST
	@Path(ApiConstants.UPDATE + ApiConstants.SYNONYM + "/{taxonId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@ValidateUser
	@Operation(summary = "Update and add synonyms to taxonomy", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = SynonymData.class))), responses = @ApiResponse(responseCode = "200", description = "Updated synonyms", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaxonomyDefinition.class)))))
	public Response updateAddSynonym(@Context HttpServletRequest request,
			@Parameter(description = "Species ID") @QueryParam("speciesId") String speciesId,
			@Parameter(description = "Taxon ID", required = true) @PathParam("taxonId") String taxonId,
			SynonymData synonymData) {
		try {
			Long sId = speciesId != null ? Long.parseLong(speciesId) : null;
			Long tId = Long.parseLong(taxonId);
			List<TaxonomyDefinition> result = taxonomyService.updateAddSynonym(request, sId, tId, synonymData);
			return Response.status(Status.OK).entity(result).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	@DELETE
	@Path(ApiConstants.REMOVE + ApiConstants.SYNONYM + "/{taxonId}/{synonymId}")
	@Consumes(MediaType.TEXT_PLAIN)
	@ValidateUser
	@Operation(summary = "Delete synonym for a taxonomy", responses = @ApiResponse(responseCode = "200", description = "List of available synonyms", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaxonomyDefinition.class)))))
	public Response removeSynonyms(@Context HttpServletRequest request,
			@Parameter(description = "Species ID") @QueryParam("speciesId") String speciesId,
			@Parameter(description = "Taxon ID", required = true) @PathParam("taxonId") String taxonId,
			@Parameter(description = "Synonym ID", required = true) @PathParam("synonymId") String synonymId) {
		try {
			Long sId = speciesId != null ? Long.parseLong(speciesId) : null;
			Long tId = Long.parseLong(taxonId);
			Long synonId = Long.parseLong(synonymId);
			List<TaxonomyDefinition> result = taxonomyService.deleteSynonym(request, sId, tId, synonId);
			return Response.status(Status.OK).entity(result).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	@PUT
	@Path(ApiConstants.ELASTIC + ApiConstants.UPDATE)
	@Consumes(MediaType.TEXT_PLAIN)
	@ValidateUser
	@Operation(summary = "Update the elastic index for taxon", responses = @ApiResponse(responseCode = "200", description = "Elastic index updated", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MapQueryResponse.class)))))
	public Response updateElastic(@Context HttpServletRequest request,
			@Parameter(description = "Comma separated Taxon IDs") @QueryParam("taxonIds") String taxonIdsString) {
		try {
			if (TaxonomyUtil.isAdmin(request)) {
				List<Long> taxonIds = Arrays.asList(taxonIdsString.split(",")).stream().map(Long::parseLong)
						.collect(Collectors.toList());
				List<MapQueryResponse> mapQueryResponses = taxonomyESOperation.pushToElastic(taxonIds);
				return Response.status(Status.OK).entity(mapQueryResponses).build();
			} else {
				throw new WebApplicationException(
						Response.status(Response.Status.UNAUTHORIZED).entity("Only admin can do the reindex").build());
			}
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build());
		}
	}

	@PUT
	@Path(ApiConstants.ELASTIC + ApiConstants.REINDEX)
	@Consumes(MediaType.TEXT_PLAIN)
	@ValidateUser
	@Operation(summary = "Reindex the complete elastic for all taxon Ids", responses = @ApiResponse(responseCode = "200", description = "Elastic index reindexed", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MapQueryResponse.class)))))
	public Response reIndexElastic(@Context HttpServletRequest request) {
		try {
			if (TaxonomyUtil.isAdmin(request)) {
				List<MapQueryResponse> mapQueryResponses = taxonomyESOperation.reIndexElastic();
				return Response.status(Status.OK).entity(mapQueryResponses).build();
			} else {
				throw new WebApplicationException(
						Response.status(Response.Status.UNAUTHORIZED).entity("Only admin can do the reindex").build());
			}
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build());
		}
	}

	@POST
	@Path(ApiConstants.ADD + ApiConstants.COMMENT)
	@Consumes(MediaType.APPLICATION_JSON)
	@ValidateUser
	@Operation(summary = "Log taxonomy Comment", requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CommentLoggingData.class))), responses = @ApiResponse(responseCode = "200", description = "Logged comment", content = @Content(schema = @Schema(implementation = Activity.class))))
	public Response addComment(@Context HttpServletRequest request, CommentLoggingData loggingData) {
		try {
			Activity result = taxonomyService.logComment(request, loggingData);
			return Response.status(Status.OK).entity(result).build();
		} catch (Exception e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	/**
	 * Only for migration purpose
	 *
	 * @param request
	 * @return
	 */
	@PUT
	@Path(ApiConstants.NAMES + ApiConstants.ITALICISED)
	@Consumes(MediaType.TEXT_PLAIN)
	@ValidateUser
	@Operation(summary = "Update Italicised form for all the taxonomy definition", responses = @ApiResponse(responseCode = "200", description = "Map of taxonomy definitions", content = @Content(schema = @Schema(implementation = Map.class))))
	public Response updateItalicisedForm(@Context HttpServletRequest request) {
		try {
			if (TaxonomyUtil.isAdmin(request)) {
				Map<String, TaxonomyDefinition> mapResponse = taxonomyService.updateItalicisedForm();
				return Response.status(Status.OK).entity(mapResponse).build();
			} else {
				throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
						.entity("Only admin can do the complete update of the name").build());
			}
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build());
		}
	}
}
