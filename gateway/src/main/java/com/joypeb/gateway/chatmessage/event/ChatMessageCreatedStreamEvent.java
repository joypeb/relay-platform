package com.joypeb.gateway.chatmessage.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatMessageCreatedStreamEvent(
	String eventId,
	String eventType,
	UUID aggregateId,
	Instant occurredAt,
	int schemaVersion,
	Payload payload,
	String traceId
) {
	@JsonIgnoreProperties(ignoreUnknown = true)
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
