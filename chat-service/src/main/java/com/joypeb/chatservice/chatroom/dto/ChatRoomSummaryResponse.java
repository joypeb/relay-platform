package com.joypeb.chatservice.chatroom.dto;

import com.joypeb.chatservice.chatroom.domain.ChatRoomVisibility;
import java.time.Instant;
import java.util.UUID;

public record ChatRoomSummaryResponse(
	UUID id,
	String name,
	String description,
	String ownerId,
	ChatRoomVisibility visibility,
	long memberCount,
	Instant createdAt
) {
}
