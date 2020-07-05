package com.dub.client.services;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.dub.client.domain.Address;
import com.dub.client.domain.EditCart;
import com.dub.client.domain.Item;
import com.dub.client.domain.Order;
import com.dub.client.domain.PaymentMethod;
import com.dub.client.exceptions.OrderNotFoundException;
import com.dub.client.exceptions.UnknownServerException;

import reactor.core.publisher.Mono;


/**
 * Try to reduce code duplication by creating a unique function and call it in flatMap
 * */

@Service
public class OrderServiceImpl implements OrderService {

	private static final String UPDATE_ORDER = "/updateOrder"; 
	private static final String CREATE_ORDER = "/createOrder"; 
	private static final String EDIT_CART = "/editCart"; 
	private static final String ORDER_BY_ID = "/orderById/"; 
	private static final String ADD_BOOK_TO_ORDER = "/addBookToOrder"; 
	private static final String GET_ACTIVE_ORDER = "/getActiveOrder"; 
	private static final String CHECKOUT_ORDER = "/checkoutOrder"; 
	private static final String SET_ORDER_STATE = "/setOrderState"; 
	
	
	@Autowired
	private WebClient orderClient;
		
	
	
	@Override
	public Order saveOrder(Order order) {
		
		Order newOrder = orderClient
				.method(HttpMethod.POST)
				.uri(UPDATE_ORDER)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.body(Mono.just(order), Order.class)
				.exchange()
				.flatMap(catchErrorsAndTransform)
				.block();
			
		return newOrder;	
	}
	
	@Override
	public Order createOrder(Order order) {
			
		Order newOrder = orderClient
				.method(HttpMethod.POST)
				.uri(CREATE_ORDER)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.body(Mono.just(order), Order.class)
				.exchange()
				.flatMap(catchErrorsAndTransform)
				.block();
		
		return newOrder;
	}

	
	@Override
	public Order addBookToOrder(String orderId, String bookId) {
				
		// call order server
		MultiValueMap<String, Object> map 
							= new LinkedMultiValueMap<>();
		map.add("orderId", orderId);
		map.add("bookId", bookId);
							    	
		Order newOrder = orderClient
				.method(HttpMethod.POST)
				.uri(ADD_BOOK_TO_ORDER)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
				.body(Mono.just(map), MultiValueMap.class)
				.exchange()
				.flatMap(catchErrorsAndTransform)
				.block();
		
		return newOrder;
	}

	
	/** caution: getActiveOrder may return null initially */
	@Override
	public Optional<Order> getActiveOrder(String userId) {
				
		Order order = orderClient
				.method(HttpMethod.POST)
				.uri(GET_ACTIVE_ORDER)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
				.body(Mono.just(userId), String.class)
				.exchange()
				.flatMap(catchErrorsAndTransformActive)
				.block();
		
		return Optional.ofNullable(order);
	}

	@Override
	public Order checkoutOrder(String orderId) {
					
		Order order = orderClient
				.method(HttpMethod.POST)
				.uri(CHECKOUT_ORDER)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
				.body(Mono.just(orderId), String.class)
				.exchange()
				.flatMap(catchErrorsAndTransform)
				.block();
		
		return order;		
	}

	
	@Override
	public Order setCart(String orderId) {
		
		MultiValueMap<String, Object> map 
						= new LinkedMultiValueMap<>();
		map.add("orderId", orderId);
		map.add("state", Order.State.CART);
	
		Order order = orderClient
				.method(HttpMethod.POST)
				.uri(SET_ORDER_STATE)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
				.body(Mono.just(map), MultiValueMap.class)
				.exchange()
				.flatMap(catchErrorsAndTransform)
				.block();
					
		return order;
	}
	
	@Override
	public Order getOrderById(String orderId) {
			
		Order order = orderClient
				.method(HttpMethod.GET)
				.uri(ORDER_BY_ID + orderId)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.exchange()
				.flatMap(catchErrorsAndTransform)
				.block();
				
		return order;		
	}

	@Override
	public Order editOrder(String orderId, List<Item> items) {
			
		// encapsulation
		EditCart editCart = new EditCart(orderId, items);
			
		Order order = orderClient
				.method(HttpMethod.POST)
				.uri(EDIT_CART)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.body(Mono.just(editCart), EditCart.class)
				.exchange()
				.flatMap(catchErrorsAndTransform)
				.block();
		
		return order;
	}
		
	@Override
	public Order setOrderState(String orderId, Order.State state) {
		
		MultiValueMap<String, Object> map 
						= new LinkedMultiValueMap<>();
		map.add("orderId", orderId);
		map.add("state", stateToString(state));

		Order order = orderClient
				.method(HttpMethod.POST)
				.uri(SET_ORDER_STATE)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.body(Mono.just(orderId), String.class)
				.exchange()
				.flatMap(catchErrorsAndTransform)
				.block();
		
		return order;
	}
		
	private String stateToString(Order.State state) {
		
		String stateStr;
		
		switch (state) {
			case CART:
				stateStr = "CART";
				break;
			case PRE_SHIPPING:
				stateStr = "PRE_SHIPPING";
				break;
			case SHIPPED:
				stateStr = "SHIPPED";
				break;
			case PRE_AUTHORIZE:
				stateStr = "PRE_AUTHORIZE";
				break;
			default:
				stateStr = "CART";
		}
		return stateStr;
	}

	@Override
	public Order finalizeOrder(Order order, Address shippingAddress, PaymentMethod payMeth) {
		
		order.setDate(new Date());
		order.setState(Order.State.PRE_SHIPPING);
		order.setPaymentMethod(payMeth);
		order.setShippingAddress(shippingAddress);
		
		return order;
	}
	
	/*
	<R> Mono<R> 	flatMap(Function<? super T,? extends Mono<? extends R>> transformer)
	Transform the item emitted by this Mono asynchronously, returning the value emitted by another Mono (possibly changing the value type).
	*/
	
	// helper function returns Mono<Order> if OK
	Function<ClientResponse, Mono<Order>> catchErrorsAndTransform = 
				(ClientResponse clientResponse) -> {
					
					if (clientResponse.statusCode().is5xxServerError()) {
						throw new UnknownServerException();
					} else if (clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)) {
						throw new OrderNotFoundException();
					} else {
						return clientResponse.bodyToMono(Order.class);
					}
				};
				
	Function<ClientResponse, Mono<Order>> catchErrorsAndTransformActive = 
				(ClientResponse clientResponse) -> {
					
					if (clientResponse.statusCode().is5xxServerError()) {
						throw new UnknownServerException();
					} else {
						return clientResponse.bodyToMono(Order.class);
					}
				};
}
