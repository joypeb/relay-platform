package com.joypeb.chatservice.chatread.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ChatReadReceiptRequest(
	@NotBlank
	String requestId,

	@NotBlank
	@Pattern(regexp = "CHAT_MESSAGES_READ")
	String type,

	@Valid
	@NotNull
	Payload payload
) {
	public record Payload(
		@Min(0)
		long lastReadSequence
	) {
	}
}
