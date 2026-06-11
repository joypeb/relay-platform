package com.joypeb.gateway.chatmessage.dto.stomp;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageBroadcastPayload(
	UUID messageId,
	UUID roomId,
	String senderId,
	long sequence,
	String type,
	String content,
	Instant createdAt
) {
}
