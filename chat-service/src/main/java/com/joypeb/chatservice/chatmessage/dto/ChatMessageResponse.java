package com.joypeb.chatservice.chatmessage.dto;

import com.joypeb.chatservice.chatmessage.domain.ChatMessageType;
import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
	UUID id,
	UUID roomId,
	String senderId,
	long sequence,
	ChatMessageType type,
	String content,
	Instant createdAt
) {
}
