package com.joypeb.gateway.websocket.connection;

import java.time.Instant;

public record StompConnectionInfo(
		String sessionId,
		String userId,
		Instant connectedAt
) {
}
