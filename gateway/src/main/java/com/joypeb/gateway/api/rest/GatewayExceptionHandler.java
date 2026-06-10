package com.joypeb.gateway.api.rest;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.joypeb.gateway.session.UnauthenticatedException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GatewayExceptionHandler {

	private final TraceIdResolver traceIdResolver;

	public GatewayExceptionHandler(TraceIdResolver traceIdResolver) {
		this.traceIdResolver = traceIdResolver;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleValidation(
			MethodArgumentNotValidException exception,
			HttpServletRequest request
	) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST,
				"Request validation failed."
		);
		problemDetail.setType(URI.create("https://joypeb.example/problems/request-validation-failed"));
		problemDetail.setTitle("Request validation failed");
		problemDetail.setInstance(URI.create(request.getRequestURI()));
		problemDetail.setProperty("code", "REQUEST_VALIDATION_FAILED");
		problemDetail.setProperty("traceId", traceIdResolver.resolve(request));
		problemDetail.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> "%s: %s".formatted(fieldError.getField(), fieldError.getDefaultMessage()))
				.toList());
		return ResponseEntity.badRequest().body(problemDetail);
	}

	@ExceptionHandler(UnauthenticatedException.class)
	public ResponseEntity<ProblemDetail> handleUnauthenticated(
			UnauthenticatedException exception,
			HttpServletRequest request
	) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
		problemDetail.setType(URI.create("https://joypeb.example/problems/unauthenticated"));
		problemDetail.setTitle("Unauthenticated");
		problemDetail.setInstance(URI.create(request.getRequestURI()));
		problemDetail.setProperty("code", "UNAUTHENTICATED");
		problemDetail.setProperty("traceId", traceIdResolver.resolve(request));
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
	}
}
