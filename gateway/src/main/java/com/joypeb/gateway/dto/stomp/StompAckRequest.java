package com.joypeb.gateway.dto.stomp;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StompAckRequest(
		@NotBlank String requestId,
		@NotBlank String type,
		Object payload,
		@NotNull Instant sentAt
) {
}
