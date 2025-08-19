/** */
package com.strandls.taxonomy.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strandls.activity.controller.ActivityServiceApi;
import com.strandls.activity.pojo.MailData;
import com.strandls.activity.pojo.SpeciesActivityLogging;
import com.strandls.activity.pojo.TaxonomyActivityLogging;
import com.strandls.taxonomy.Headers;

import jakarta.inject.Inject;

/**
 * @author Abhishek Rudra
 */
public class LogActivities {

	@Inject
	private ActivityServiceApi activityService;

	@Inject
	private Headers headers;

	private final Logger logger = LoggerFactory.getLogger(LogActivities.class);

	public void logSpeciesActivity(String authHeader, String activityDescription, Long rootObjectId,
			Long subRootObjectId, String rootObjectType, Long activityId, String activityType, MailData mailData) {

		try {
			SpeciesActivityLogging activityLogging = new SpeciesActivityLogging();
			activityLogging.setActivityDescription(activityDescription);
			activityLogging.setActivityId(activityId);
			activityLogging.setActivityType(activityType);
			activityLogging.setRootObjectId(rootObjectId);
			activityLogging.setRootObjectType(rootObjectType);
			activityLogging.setSubRootObjectId(subRootObjectId);
			activityLogging.setMailData(mailData);
			activityService = headers.addActivityHeader(activityService, authHeader);
			activityService.logSpeciesActivities(activityLogging);

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	public void logTaxonomyActivities(String authHeader, String activityDescription, Long rootObjectId,
			Long subRootObjectId, String rootObjectType, Long activityId, String activityType) {
		try {

			TaxonomyActivityLogging activityLogging = new TaxonomyActivityLogging();
			activityLogging.setActivityDescription(activityDescription);
			activityLogging.setActivityId(activityId);
			activityLogging.setActivityType(activityType);
			activityLogging.setRootObjectId(rootObjectId);
			activityLogging.setRootObjectType(rootObjectType);
			activityLogging.setSubRootObjectId(subRootObjectId);
			activityService = headers.addActivityHeader(activityService, authHeader);
			activityService.logTaxonomyActivities(activityLogging);

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}
}
