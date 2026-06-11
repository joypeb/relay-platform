package com.joypeb.gateway.chatmessage.dto.stomp;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ChatMessageStompSendRequest(
	@NotBlank
	@Size(max = 64)
	String requestId,

	@NotBlank
	String type,

	@Valid
	@NotNull
	Payload payload,

	@NotNull
	Instant sentAt
) {
	public record Payload(
		@NotBlank
		@Size(max = 2000)
		String content
	) {
	}
}
