package com.dub.client.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.dub.client.domain.Category;

import reactor.core.publisher.Flux;



@Service
public class CategoryServiceImpl implements CategoryService {

	private static final String ALL_CATEGORIES = "/allCategories";
	private static final String CATEGORY = "/category/";
	
	@Autowired
	private WebClient bookClient;
	
	
	@Override
	public List<Category> getLeaveCategories() {
		
		WebClient.RequestBodySpec requestSpec = bookClient
				.method(HttpMethod.GET)
				.uri(ALL_CATEGORIES);
		
		WebClient.ResponseSpec response = requestSpec.retrieve();
	
		Flux<Category> flux  = response.bodyToFlux(Category.class);
	
		List<Category> cats = flux.collectList().block();
	
		List<Category> leaves = new ArrayList<>();
			
		// extract leaves
		for (Category cat : cats) {
			if (cat.getChildren().isEmpty()) {
						leaves.add(cat);
			}
		}
					
		return leaves;				
	}
	
	
	@Override
	public Category getCategory(String categorySlug) {
		Category cat = bookClient
				.method(HttpMethod.GET)
				.uri(CATEGORY + categorySlug)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.retrieve().bodyToMono(Category.class).block();
	
		return cat;
	}
}
