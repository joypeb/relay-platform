package com.joypeb.chatservice.chatmessage.event;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageCreatedEvent(
	String eventId,
	String eventType,
	UUID aggregateId,
	Instant occurredAt,
	int schemaVersion,
	Payload payload,
	String traceId
) {
	public static ChatMessageCreatedEvent from(
		String eventId,
		UUID messageId,
		UUID roomId,
		String senderId,
		long sequence,
		String type,
		String content,
		Instant createdAt,
		Instant occurredAt,
		String traceId
	) {
		return new ChatMessageCreatedEvent(
			eventId,
			"CHAT_MESSAGE_CREATED",
			roomId,
			occurredAt,
			1,
			new Payload(messageId, roomId, senderId, sequence, type, content, createdAt),
			traceId
		);
	}

	public record Payload(
		UUID messageId,
		UUID roomId,
		String senderId,
		long sequence,
		String type,
		String content,
		Instant createdAt
	) {
	}
}
