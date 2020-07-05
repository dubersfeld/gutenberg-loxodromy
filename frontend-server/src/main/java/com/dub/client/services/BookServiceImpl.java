package com.dub.client.services;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.dub.client.domain.Book;
import com.dub.client.domain.BookSearch;
import com.dub.client.domain.BookWebList;
import com.dub.client.domain.Review;
import com.dub.client.domain.UserAndReviewedBooks;
import com.dub.client.exceptions.BookNotFoundException;
import com.dub.client.exceptions.UnknownServerException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
public class BookServiceImpl implements BookService {
	
	private static final String BOOKS = "/books/";
	private static final String BOOK_BY_ID = "/bookById/";
	private static final String BOOKS_BOUGHT_WITH = "/booksBoughtWith/";
	private static final String SORT = "/sort/";
	private static final String OUT_LIMIT = "/outLimit/";
	private static final String BOOKS_NOT_REVIEWED = "/getBooksNotReviewed";
	
	
	@Autowired
	private WebClient orderClient;
	
	@Autowired
	private WebClient bookClient;
			
	@Autowired
	private ReviewService reviewService;

	
	@Override
	public Book getBookBySlug(String slug) {
		
		Book book = bookClient
		.method(HttpMethod.GET)
		.uri(BOOKS + slug)
		.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
		.exchange()
		.flatMap(catchErrorsAndTransform)
		.block();
		
		return book;
	}
	
	
	@Override
	public Book getBookById(String id) {
	
		Book book = bookClient
		.method(HttpMethod.GET)
		.uri(BOOK_BY_ID + id)
		.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
		.exchange()
		.flatMap(catchErrorsAndTransform)
		.block();
		
		return book;
	}
	
	
	public List<Book> allBooksByCategory(String categorySlug, String sortBy) {
				
		WebClient.RequestBodySpec requestSpec = bookClient
				.method(HttpMethod.GET)		
				.uri(BOOKS + categorySlug + SORT + sortBy);
		
				WebClient.ResponseSpec response = requestSpec.retrieve();
	
				Flux<Book> books  = response.bodyToFlux(Book.class);
	
				List<Book> blist = books.collectList().block();
	
				return blist;
	}
	
	
	@Override
	public List<Book> getBooksBoughtWith(String bookId, int outLimit) {
	
		
		WebClient.RequestBodySpec requestSpec = bookClient
				.method(HttpMethod.GET)		
				.uri(BOOKS_BOUGHT_WITH + bookId + OUT_LIMIT + outLimit);

				WebClient.ResponseSpec response = requestSpec.retrieve();
				Flux<Book> books  = response.bodyToFlux(Book.class);	
				List<Book> blist = books.collectList().block();
	
				return blist;
	
	}

		
	@Override
	public List<Book> getBooksNotReviewed(String userId, int outLimit) throws ParseException {
				
		/** 
		 * first step: find all books 
		 * already reviewed by user referenced by userId 
		 * */
				
		List<Review> reviews = reviewService.getReviewsByUserId(userId);
					
		List<String> reviewedBookIds = new ArrayList<>();
			
		for (Review review : reviews) {
			reviewedBookIds.add(review.getBookId().toString());
		}
				
		/** 
		 * second step: find all books 
		 * recently bought by user referenced by userId 
		 * that were not reviewed by user yet.
		 * this step is implemented on order server side
		 * we need to post a List<String> to order server
		 * it is easier here to post an encapsulating object
		 * the helper class is named UserAndReviews
		 * */
			
		UserAndReviewedBooks userAndReviewedBooks 
					= new UserAndReviewedBooks(
									userId, 
									reviewedBookIds, 
									outLimit);
							
		BookWebList bookList = orderClient
					.method(HttpMethod.POST)
					.uri(BOOKS_NOT_REVIEWED)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.body(Mono.just(userAndReviewedBooks), UserAndReviewedBooks.class)		
					.exchange()
					.flatMap(catchErrorsAndTransformList)
					.block();
								
		List<Book> booksToReview = new ArrayList<>();
		for (String bookId : bookList.getBookIds()) {	
				Book book = this.getBookById(bookId); 
				booksToReview.add(book);
		}
			
		return booksToReview;
	}
	
	
	@Override
	public List<Book> searchBookByTitle(String searchString) {
					
		return this.searchBook(searchString, "/searchByTitle");
	}
	
	
	@Override
	public List<Book> searchBookByDescription(String searchString) {
				
		//String booksURI = baseBooksUrl + "/searchByDescription";
		
		return this.searchBook(searchString, "/searchByDescription");
	}
	
	@Override
	public List<Book> searchBookByTags(String searchString) {
				
		//String booksURI = baseBooksUrl + "/searchByTags";
		
		return this.searchBook(searchString, "/searchByTags");
	}
	
	
	private List<Book> searchBook(String searchString, String booksURI) {
			
		BookSearch bookSearch = new BookSearch();
		bookSearch.setSearchString(searchString);
				
		WebClient.ResponseSpec responseSpec = bookClient
				.method(HttpMethod.POST)		
				.uri(booksURI)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
		.body(Mono.just(bookSearch), UserAndReviewedBooks.class)		
		.retrieve();
		
		Flux<Book> books = responseSpec.bodyToFlux(Book.class);
		
		List<Book> blist = books.collectList().block();
		
		return blist;	
	}
	
	
	/** helper functions used as transformers */
	
	
	// helper function returns Mono<Book> if OK
	Function<ClientResponse, Mono<Book>> catchErrorsAndTransform = 
					(ClientResponse clientResponse) -> {
						if (clientResponse.statusCode().is5xxServerError()) {
							throw new UnknownServerException();
						} else if (clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)) {
							throw new BookNotFoundException();
						} else {
							return clientResponse.bodyToMono(Book.class);
						}
					};
					
	// helper function returns Mono<BookWebList> if OK
	Function<ClientResponse, Mono<BookWebList>> catchErrorsAndTransformList = 
					(ClientResponse clientResponse) -> {
						if (clientResponse.statusCode().is5xxServerError()) {
							throw new UnknownServerException();				
						} else if (clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)) {
							throw new BookNotFoundException();
						} else {
							return clientResponse.bodyToMono(BookWebList.class);
						}
					};

}
