package com.joypeb.gateway.chatmessage.dto.stomp;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

public record ChatReadReceiptStompRequest(
	@NotBlank
	String requestId,

	@NotBlank
	@Pattern(regexp = "CHAT_MESSAGES_READ")
	String type,

	@Valid
	@NotNull
	Payload payload,

	@NotNull
	Instant sentAt
) {
	public record Payload(
		@Min(0)
		long lastReadSequence
	) {
	}
}
