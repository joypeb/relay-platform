package com.joypeb.gateway.session;

public class UnauthenticatedException extends RuntimeException {

	public UnauthenticatedException(String message) {
		super(message);
	}
}
