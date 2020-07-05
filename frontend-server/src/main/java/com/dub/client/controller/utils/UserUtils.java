package com.dub.client.controller.utils;

import javax.servlet.http.HttpSession;

import com.dub.client.domain.AccountCreation;
import com.dub.client.domain.MyUser;

public interface UserUtils {

	public MyUser getLoggedUser(HttpSession session);
	
	public void setLoggedUser(HttpSession session, MyUser user);
	
	public MyUser reload(HttpSession session);
	
	/*
	public MyUser createUser(String username, String password, 
			List<Address> addresses, List<PaymentMethod> paymentMethods,
			UserService userService);
	*/
	public MyUser createUser(AccountCreation account);
}