package com.joypeb.gateway.dto.stomp;

import java.time.Instant;

public record StompServerEvent<T>(
		String eventId,
		String type,
		T payload,
		Instant occurredAt,
		String traceId
) {
}
