package com.joypeb.chatservice.chatmessage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatMessageSendRequest(
	@NotBlank
	@Size(max = 64)
	String requestId,

	@NotBlank
	@Pattern(regexp = "TEXT")
	String type,

	@NotBlank
	@Size(max = 2000)
	String content
) {
}
