package com.joypeb.gateway.websocket.connection;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

@Component
public class StompConnectionRegistry {

	private final Clock clock;
	private final ConcurrentMap<String, StompConnectionInfo> connections = new ConcurrentHashMap<>();

	public StompConnectionRegistry(Clock clock) {
		this.clock = clock;
	}

	public void connect(String sessionId, String userId) {
		connections.put(sessionId, new StompConnectionInfo(sessionId, userId, Instant.now(clock)));
	}

	public void disconnect(String sessionId) {
		connections.remove(sessionId);
	}

	public long activeConnectionCount() {
		return connections.size();
	}

	public Optional<StompConnectionInfo> findBySessionId(String sessionId) {
		return Optional.ofNullable(connections.get(sessionId));
	}
}
