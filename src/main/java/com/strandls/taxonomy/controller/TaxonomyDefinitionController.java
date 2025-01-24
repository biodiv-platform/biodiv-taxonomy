/**
 * 
 */
package com.strandls.taxonomy.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * @author Abhishek Rudra
 *
 */
@Api("Taxonomy Services")
@Path(ApiConstants.V1 + ApiConstants.TAXONOMY)
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
	@Produces(MediaType.APPLICATION_JSON)

	@ApiOperation(value = "Find Taxonomy by ID", notes = "Returns Taxonomy details", response = TaxonomyDefinition.class)
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Taxonomy not found", response = String.class) })

	public Response getTaxonomyConceptName(@PathParam("taxonomyConceptId") String taxonomyConceptId) {
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
	@Produces(MediaType.APPLICATION_JSON)

	@ApiOperation(value = "Find Taxonomy Details by ID", notes = "Returns Taxonomy details", response = TaxonomyDefinitionShow.class)
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Taxonomy not found", response = String.class) })

	public Response getTaxonomyDetails(@PathParam("taxonId") String taxonId) {
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
	@Produces(MediaType.APPLICATION_JSON)

	@ApiOperation(value = "Get taxonomy name list ", notes = "Returns taxonomy details", response = TaxonomyNameListResponse.class)
	@ApiResponses(value = { @ApiResponse(code = 404, message = "Taxonomy list not found", response = String.class) })
	public Response getTaxonomyNameList(@Context HttpServletRequest request, @QueryParam("taxonId") Long taxonId,
			@QueryParam("classificationId") Long classificationId, @QueryParam("rankList") String rankList,
			@QueryParam("statusList") String statusList, @QueryParam("positionList") String positionList,
			@DefaultValue("-1") @QueryParam("limit") Integer limit,
			@DefaultValue("-1") @QueryParam("offset") Integer offset) {
		try {
			TaxonomyNameListResponse response = taxonomyService.getTaxonomyNameList(taxonId, classificationId, rankList,
					statusList, positionList, limit, offset);
			return Response.ok().entity(response).build();
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
		}
	}

	@Path("upload")
	@POST
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Upload the file for taxon definition", notes = "Returns succuess failure", response = FileMetadata.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "file not present", response = String.class),
			@ApiResponse(code = 500, message = "ERROR", response = String.class) })
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

	@Path("upload/search")
	@POST
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Upload the file for taxon definition", notes = "Returns succuess failure", response = Map.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "file not present", response = String.class),
			@ApiResponse(code = 500, message = "ERROR", response = String.class) })
	public Response uploadSearch(final FormDataMultiPart multiPart) {
		FormDataBodyPart filePart = multiPart.getField("file");
		if (filePart == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("File not present").build();
		} else {
			Map<String, Object> result;
			try {
				result = taxonomyService.nameMatching(filePart);
				return Response.ok().entity(result).build();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
			}
		}
	}

	@POST
	@Path("list")
	@Produces(MediaType.APPLICATION_JSON)
	@ValidateUser
	@ApiOperation(value = "save the taxonomy list", notes = "return the saved taxonomy", response = TaxonomyDefinition.class, responseContainer = "List")
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "failed to save the taxon definition", response = String.class) })
	public Response saveTaxonomyList(@Context HttpServletRequest request,
			@ApiParam("taxonomyList") List<TaxonomySave> taxonomyList) {
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
	@ApiOperation(value = "save the taxonomy", notes = "return the saved taxonomy", response = TaxonomyDefinition.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "failed to save the taxon definition", response = String.class) })
	public Response saveTaxonomy(@Context HttpServletRequest request,
			@ApiParam("taxonSave") TaxonomySave taxonomySave) {
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
	@ApiOperation(value = "Get taxonomy based on the canonical name and rank", notes = "return the found taxonomy", response = TaxonomySearch.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "failed to get the taxon definition", response = String.class) })
	public Response getByNameSearch(@QueryParam("scientificName") String scientificName,
			@QueryParam("rankName") String rankName) {
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
	@ApiOperation(value = "Search taxonomy based on the name", notes = "return the found taxonomy", response = Object.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "failed to get the taxon definition", response = String.class) })
	public Response search(@QueryParam("term") String term) {
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
	@ApiOperation(value = "Search taxonomy based on the Ids", notes = "return the found taxonomy", response = Object.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "failed to get the taxon definition", response = String.class) })
	public Response specificSearch(@QueryParam("term") String term, @QueryParam("classification") Long classificationId,
			@QueryParam("taxonid") Long taxonid) {
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
	@Produces(MediaType.APPLICATION_JSON)

	@ApiOperation(value = "get the common name and synonyms", notes = "return taxonoicNames based on taxonomyId", response = TaxonomicNames.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "unable to get the names", response = String.class) })

	public Response getNames(@PathParam("taxonomyId") String taxonomyId) {
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
	@Produces(MediaType.APPLICATION_JSON)

	@ValidateUser

	@ApiOperation(value = "Update the name of taxonomy", notes = "Update the name. input name should be scientific name", response = TaxonomyDefinitionShow.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "failed to update the name of taxonomy definition", response = String.class) })
	public Response updateName(@Context HttpServletRequest request, @QueryParam("taxonId") Long taxonId,
			@QueryParam("taxonName") String taxonName) {
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
	@Produces(MediaType.APPLICATION_JSON)

	@ValidateUser

	@ApiOperation(value = "Update the name of taxonomy", notes = "Update the status. Status should be either accepted or synonym", response = TaxonomyDefinitionShow.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "failed to update the name of taxonomy definition", response = String.class) })
	public Response updateStatus(@Context HttpServletRequest request,
			@ApiParam("status") TaxonomyStatusUpdate taxonomyStatusUpdate) {
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
	@Produces(MediaType.APPLICATION_JSON)

	@ValidateUser

	@ApiOperation(value = "Update the position of taxonomy", notes = "Update the position. Position should be raw or working", response = TaxonomyDefinition.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "failed to update the position of taxonomy definition", response = String.class) })
	public Response updatePosition(@Context HttpServletRequest request,
			@ApiParam("status") TaxonomyPositionUpdate taxonomyPositionUpdate) {
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
	@Produces(MediaType.APPLICATION_JSON)

	@ValidateUser

	@ApiOperation(value = "update and add synonyms", notes = "return synonyms based on taxonomyId", response = TaxonomyDefinition.class, responseContainer = "List")
	@ApiResponses(value = { @ApiResponse(code = 400, message = "unable to add the names", response = String.class) })

	public Response updateAddSynonym(@Context HttpServletRequest request, @QueryParam("speciesId") String speciesId,
			@PathParam("taxonId") String taxonId, @ApiParam(name = "synonymData") SynonymData synonymData) {
		try {
			Long sId = null;
			if (speciesId != null)
				sId = Long.parseLong(speciesId);
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
	@Produces(MediaType.APPLICATION_JSON)

	@ValidateUser

	@ApiOperation(value = "delete synonyms", notes = "return list of avaible synonyms", response = TaxonomyDefinition.class, responseContainer = "List")
	@ApiResponses(value = { @ApiResponse(code = 400, message = "unable to delete the names", response = String.class) })

	public Response removeSynonyms(@Context HttpServletRequest request, @QueryParam("speciesId") String speciesId,
			@PathParam("taxonId") String taxonId, @PathParam("synonymId") String synonymId) {
		try {
			Long sId = null;
			if (speciesId != null)
				sId = Long.parseLong(speciesId);
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
	@Produces(MediaType.APPLICATION_JSON)

	@ValidateUser

	@ApiOperation(value = "Update the elastic index for taxon", notes = "Re-index the elastic for given taxon Ids", response = TaxonomyDefinition.class, responseContainer = "List")
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "unable to reIndex the taxon Ids", response = String.class) })

	public Response updateElastic(@Context HttpServletRequest request, @QueryParam("taxonIds") String taxonIdsString) {
		try {
			if (TaxonomyUtil.isAdmin(request)) {
				List<Long> taxonIds = Arrays.asList(taxonIdsString.split(",")).stream().map(Long::parseLong)
						.collect(Collectors.toList());
				List<MapQueryResponse> mapQueryResponses = taxonomyESOperation.pushToElastic(taxonIds);
				return Response.status(Status.OK).entity(mapQueryResponses).build();
			} else
				throw new WebApplicationException(
						Response.status(Response.Status.UNAUTHORIZED).entity("Only admin can do the reindex").build());
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build());
		}
	}

	@PUT
	@Path(ApiConstants.ELASTIC + ApiConstants.REINDEX)
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)

	@ValidateUser

	@ApiOperation(value = "Update the elastic index for taxon", notes = "Re-index the complete elastic for all taxon Ids", response = TaxonomyDefinition.class, responseContainer = "List")
	@ApiResponses(value = { @ApiResponse(code = 400, message = "unable to delete the names", response = String.class) })

	public Response reIndexElastic(@Context HttpServletRequest request) {
		try {
			if (TaxonomyUtil.isAdmin(request)) {
				List<MapQueryResponse> mapQueryResponses = taxonomyESOperation.reIndexElastic();
				return Response.status(Status.OK).entity(mapQueryResponses).build();
			} else
				throw new WebApplicationException(
						Response.status(Response.Status.UNAUTHORIZED).entity("Only admin can do the reindex").build());
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build());
		}
	}

	@POST
	@Path(ApiConstants.ADD + ApiConstants.COMMENT)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)

	@ValidateUser

	@ApiOperation(value = "log taxonomy Comment", notes = "Return the logged the comment", response = Activity.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "unable to log comment", response = String.class) })

	public Response addComment(@Context HttpServletRequest request,
			@ApiParam(name = "loggingData") CommentLoggingData loggingData) {
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
	@Produces(MediaType.APPLICATION_JSON)

	@ValidateUser

	@ApiOperation(value = "Update Italicised form for all the taxonomy definition", response = Map.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "unable to upate the italicised form for the names", response = String.class) })

	public Response updateItalicisedForm(@Context HttpServletRequest request) {
		try {
			if (TaxonomyUtil.isAdmin(request)) {
				Map<String, TaxonomyDefinition> mapResponse = taxonomyService.updateItalicisedForm();
				return Response.status(Status.OK).entity(mapResponse).build();
			} else
				throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
						.entity("Only admin can do the complete update of the name").build());
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build());
		}
	}

}
