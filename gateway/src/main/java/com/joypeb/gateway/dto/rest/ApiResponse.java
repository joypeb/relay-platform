package com.joypeb.gateway.dto.rest;

import java.time.Instant;

public record ApiResponse<T>(
		boolean success,
		T data,
		String traceId,
		Instant timestamp
) {

	public static <T> ApiResponse<T> success(T data, String traceId, Instant timestamp) {
		return new ApiResponse<>(true, data, traceId, timestamp);
	}
}
