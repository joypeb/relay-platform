package com.joypeb.gateway.chatmessage.dto.stomp;

import java.time.Instant;

public record ChatMessageStompErrorPayload(
	String requestId,
	String code,
	String message,
	String traceId,
	Instant occurredAt
) {
}
