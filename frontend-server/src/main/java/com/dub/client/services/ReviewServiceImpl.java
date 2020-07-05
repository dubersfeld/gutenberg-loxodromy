package com.dub.client.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

import com.dub.client.domain.Review;
import com.dub.client.domain.ReviewVote;
import com.dub.client.exceptions.ReviewNotFoundException;
import com.dub.client.exceptions.UnknownServerException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
public class ReviewServiceImpl implements ReviewService {
			
	public static final String CREATE_REVIEW = "/createReview";
	public static final String REVIEWS_BY_BOOK_ID = "/reviewsByBookId/";
	public static final String BOOK_RATING = "/bookRating/";
	public static final String REVIEW_BY_ID = "/reviewById/";
	public static final String REVIEWS_BY_USER_ID = "/reviewsByUserId";
	public static final String ADD_VOTE = "/addVote/";
	public static final String SORT = "/sort/";
	

	@Autowired 
	private WebClient reviewClient;
	
	
	@Override
	public Review createReview(Review review) {
		
		reviewClient
			.method(HttpMethod.POST)
			.uri(CREATE_REVIEW)
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.body(Mono.just(review), Review.class)
			.exchange()
			.flatMap(catchErrorsAndTransform)
			.block();
		
		return review;
		
	}
	
	
	@Override
	public List<Review> getReviewsByBookId(String bookId, String sortBy) {

		WebClient.RequestBodySpec requestSpec = reviewClient
				.method(HttpMethod.GET)
				.uri(REVIEWS_BY_BOOK_ID + bookId + SORT + sortBy);
		
		WebClient.ResponseSpec response = requestSpec.retrieve();
		Flux<Review> flux  = response.bodyToFlux(Review.class);
		List<Review> rlist = flux.collectList().block();
	
		return rlist;
	
	}

	
	public Optional<Double> getBookRating(String bookId) {
	
		Double rating = reviewClient
				.method(HttpMethod.GET)
				.uri(BOOK_RATING + bookId)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.retrieve().bodyToMono(Double.class).block();
		
		return Optional.ofNullable(rating);
	}
	
	
	public boolean hasVoted(String reviewId, String userId) {
		
		Review review = reviewClient
				.method(HttpMethod.GET)
				.uri(REVIEW_BY_ID + reviewId)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.retrieve().bodyToMono(Review.class).block();
		
		Set<String> voterIds = review.getVoterIds();
		
		return voterIds.contains(userId);
	}
	

	@Override
	public void voteHelpful(String reviewId, String userId, boolean helpful) {
	
		ReviewVote reviewVote = new ReviewVote();
		reviewVote.setHelpful(helpful);
		reviewVote.setUserId(userId);
		
		reviewClient
			.method(HttpMethod.POST)
			.uri(ADD_VOTE + reviewId)
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.body(Mono.just(reviewVote), Boolean.class)
			.exchange()
			.flatMap(catchErrorsAndTransform)
			.block();
			
	}
	
	
	@Override
	public List<Review> getReviewsByUserId(String userId) {

		ResponseSpec response = reviewClient
		.method(HttpMethod.POST)
		.uri(REVIEWS_BY_USER_ID)
		.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
		.body(Mono.just(userId), String.class).retrieve();
		
		Flux<Review> flux = response.bodyToFlux(Review.class);
		
		List<Review> rlist = flux.collectList().block();
		
		return rlist;
		
	}
	
	// helper function returns Mono<Review> if OK
	Function<ClientResponse, Mono<Review>> catchErrorsAndTransform = 
				(ClientResponse clientResponse) -> {
					if (clientResponse.statusCode().is5xxServerError()) {
						throw new UnknownServerException();
					} else if (clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)) {
						throw new ReviewNotFoundException();
					} else {
						return clientResponse.bodyToMono(Review.class);
					}
				};
	
}
