package com.dub.client;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import com.dub.client.controller.config.GutenbergProperties;

@SpringBootApplication
@EnableConfigurationProperties(GutenbergProperties.class)
public class Application {

	private static final Logger logger = LoggerFactory.getLogger(Application.class);	

	@Autowired
	private GutenbergProperties gutenbergProperties;
	
	
	public static void main(String[] args) {
		
		SpringApplication.run(Application.class, args);
		
		logger.debug("--Application started--");
	
	}
	
	/*
	@Bean 
	public RestOperations restTemplate() {
		return new RestTemplate();
	}
	*/
	
	
	@Bean 
	WebClient bookClient() {
		
		// create a dedicated WebClient for each service
		WebClient client = WebClient.builder()
								.baseUrl(gutenbergProperties.getBaseBooksUrl())			
								.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) 
								.defaultUriVariables(Collections.singletonMap("url", gutenbergProperties.getBaseBooksUrl()))
								.build();
		return client;
	}
	
	@Bean 
	WebClient reviewClient() {
		
		// create a dedicated WebClient for each service
		WebClient client = WebClient.builder()
								.baseUrl(gutenbergProperties.getBaseReviewsUrl())
								.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) 
								.defaultUriVariables(Collections.singletonMap("url", gutenbergProperties.getBaseReviewsUrl()))
								.build();
		return client;
	}
	
	@Bean 
	WebClient orderClient() {
		
		// create a dedicated WebClient for each service
		WebClient client = WebClient.builder()			
								.baseUrl(gutenbergProperties.getBaseOrdersUrl())
								.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) 
								.defaultUriVariables(Collections.singletonMap("url", this.gutenbergProperties.getBaseOrdersUrl()))
								.build();
		return client;
	}
	
	@Bean 
	WebClient userClient() {
		
		// create a dedicated WebClient for each service
		WebClient client = WebClient.builder()
								.baseUrl(gutenbergProperties.getBaseUsersUrl())
								.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) 
								.defaultUriVariables(Collections.singletonMap("url", gutenbergProperties.getBaseUsersUrl()))
								.build();
		return client;
	}
	
	
}

