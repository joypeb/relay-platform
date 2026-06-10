package com.joypeb.chatservice.chatroom.dto;

import com.joypeb.chatservice.chatroom.domain.ChatRoomStatus;
import com.joypeb.chatservice.chatroom.domain.ChatRoomVisibility;
import java.time.Instant;
import java.util.UUID;

public record ChatRoomResponse(
	UUID id,
	String name,
	String description,
	String ownerId,
	ChatRoomVisibility visibility,
	ChatRoomStatus status,
	long memberCount,
	Instant createdAt,
	Instant updatedAt
) {
}
