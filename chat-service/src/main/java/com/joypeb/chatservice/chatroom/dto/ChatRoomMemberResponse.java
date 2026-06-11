package com.joypeb.chatservice.chatroom.dto;

import com.joypeb.chatservice.chatroom.domain.ChatRoomMemberRole;
import java.time.Instant;
import java.util.UUID;

public record ChatRoomMemberResponse(
	UUID id,
	UUID roomId,
	String memberId,
	ChatRoomMemberRole role,
	Instant joinedAt
) {
}
