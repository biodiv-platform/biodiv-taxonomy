/** */
package com.strandls.taxonomy;

import com.strandls.activity.controller.ActivityServiceApi;

import jakarta.ws.rs.core.HttpHeaders;

/**
 * @author Abhishek Rudra
 */
public class Headers {

	public ActivityServiceApi addActivityHeader(ActivityServiceApi activityService, String authHeaders) {
		activityService.getApiClient().addDefaultHeader(HttpHeaders.AUTHORIZATION, authHeaders);
		return activityService;
	}
}
