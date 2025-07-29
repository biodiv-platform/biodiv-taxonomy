/**
 *
 */
package com.strandls.taxonomy;

import jakarta.ws.rs.core.HttpHeaders;

import com.strandls.activity.controller.ActivityServiceApi;

/**
 * @author Abhishek Rudra
 *
 *
 */
public class Headers {

	public ActivityServiceApi addActivityHeader(ActivityServiceApi activityService, String authHeaders) {
		activityService.getApiClient().addDefaultHeader(HttpHeaders.AUTHORIZATION, authHeaders);
		return activityService;
	}
}
