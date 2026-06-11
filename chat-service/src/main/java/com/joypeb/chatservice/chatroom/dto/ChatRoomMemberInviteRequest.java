package com.joypeb.chatservice.chatroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChatRoomMemberInviteRequest(
	@NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{1,64}$") String memberId
) {
}
