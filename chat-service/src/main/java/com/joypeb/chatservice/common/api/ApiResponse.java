package com.joypeb.chatservice.common.api;

import java.time.Instant;

public record ApiResponse<T>(
	boolean success,
	T data,
	String traceId,
	Instant timestamp
) {
	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, data, null, Instant.now());
	}
}
