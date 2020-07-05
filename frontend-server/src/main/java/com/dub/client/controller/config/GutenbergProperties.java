package com.dub.client.controller.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


//@Component
@ConfigurationProperties(prefix = "gutenberg")
public class GutenbergProperties {

	private String baseBooksUrl;
	private String baseReviewsUrl;
	// http://localhost:5555/reviews
	private String baseOrdersUrl;//: http://localhost:5555/orders
	private	String baseUsersUrl;
	public String getBaseBooksUrl() {
		return baseBooksUrl;
	}
	public void setBaseBooksUrl(String baseBooksUrl) {
		this.baseBooksUrl = baseBooksUrl;
	}
	public String getBaseReviewsUrl() {
		return baseReviewsUrl;
	}
	public void setBaseReviewsUrl(String baseReviewsUrl) {
		this.baseReviewsUrl = baseReviewsUrl;
	}
	public String getBaseOrdersUrl() {
		return baseOrdersUrl;
	}
	public void setBaseOrdersUrl(String baseOrdersUrl) {
		this.baseOrdersUrl = baseOrdersUrl;
	}
	public String getBaseUsersUrl() {
		return baseUsersUrl;
	}
	public void setBaseUsersUrl(String baseUsersUrl) {
		this.baseUsersUrl = baseUsersUrl;
	}
	
	

}
