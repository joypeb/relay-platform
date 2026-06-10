package com.joypeb.gateway.api.rest;

import java.time.Clock;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.joypeb.gateway.dto.rest.ApiResponse;
import com.joypeb.gateway.dto.rest.LoginRequest;
import com.joypeb.gateway.dto.rest.SessionResponse;
import com.joypeb.gateway.session.SessionAttributes;
import com.joypeb.gateway.session.UnauthenticatedException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

	private final Clock clock;
	private final TraceIdResolver traceIdResolver;

	public SessionController(Clock clock, TraceIdResolver traceIdResolver) {
		this.clock = clock;
		this.traceIdResolver = traceIdResolver;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<SessionResponse>> create(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest servletRequest
	) {
		HttpSession session = servletRequest.getSession(true);
		session.setAttribute(SessionAttributes.AUTHENTICATED_USER_ID, request.userId());

		SessionResponse response = new SessionResponse(request.userId(), session.getId());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(response, traceIdResolver.resolve(servletRequest), Instant.now(clock)));
	}

	@GetMapping("/current")
	public ResponseEntity<ApiResponse<SessionResponse>> current(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			throw new UnauthenticatedException("Authentication is required.");
		}

		Object userId = session.getAttribute(SessionAttributes.AUTHENTICATED_USER_ID);
		if (!(userId instanceof String authenticatedUserId)) {
			throw new UnauthenticatedException("Authentication is required.");
		}

		SessionResponse response = new SessionResponse(authenticatedUserId, session.getId());
		return ResponseEntity.ok(ApiResponse.success(response, traceIdResolver.resolve(request), Instant.now(clock)));
	}

	@DeleteMapping("/current")
	public ResponseEntity<Void> deleteCurrent(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		return ResponseEntity.noContent().build();
	}
}
