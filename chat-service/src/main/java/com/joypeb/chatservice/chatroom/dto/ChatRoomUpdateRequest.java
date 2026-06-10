package com.joypeb.chatservice.chatroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRoomUpdateRequest(
	@NotBlank @Size(max = 100) String name,
	@Size(max = 500) String description,
	boolean publiclyVisible
) {
}
