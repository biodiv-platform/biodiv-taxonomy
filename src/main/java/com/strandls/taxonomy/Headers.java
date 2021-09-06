/**
 * 
 */
package com.strandls.taxonomy;

import javax.ws.rs.core.HttpHeaders;

import com.strandls.activity.controller.ActivitySerivceApi;

/**
 * @author Abhishek Rudra
 *
 * 
 */
public class Headers {

	public ActivitySerivceApi addActivityHeader(ActivitySerivceApi activityService, String authHeaders) {
		activityService.getApiClient().addDefaultHeader(HttpHeaders.AUTHORIZATION, authHeaders);
		return activityService;
	}
}
