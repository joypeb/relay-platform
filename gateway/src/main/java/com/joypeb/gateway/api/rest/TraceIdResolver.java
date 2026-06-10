package com.joypeb.gateway.api.rest;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class TraceIdResolver {

	public static final String TRACE_ID_HEADER = "X-Trace-Id";

	public String resolve(HttpServletRequest request) {
		return Optional.ofNullable(request.getHeader(TRACE_ID_HEADER))
				.filter(StringUtils::hasText)
				.orElseGet(() -> UUID.randomUUID().toString());
	}
}
