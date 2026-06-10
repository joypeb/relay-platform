package com.joypeb.gateway.websocket.connection;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class StompSessionEventListener {

	private static final Logger log = LoggerFactory.getLogger(StompSessionEventListener.class);

	private final StompConnectionRegistry connectionRegistry;

	public StompSessionEventListener(StompConnectionRegistry connectionRegistry) {
		this.connectionRegistry = connectionRegistry;
	}

	@EventListener
	public void onConnect(SessionConnectEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		Principal user = accessor.getUser();
		String sessionId = accessor.getSessionId();
		if (user == null || sessionId == null) {
			return;
		}

		connectionRegistry.connect(sessionId, user.getName());
		log.info("stomp_connected sessionId={} userId={}", sessionId, user.getName());
	}

	@EventListener
	public void onDisconnect(SessionDisconnectEvent event) {
		String sessionId = event.getSessionId();
		connectionRegistry.disconnect(sessionId);
		log.info("stomp_disconnected sessionId={}", sessionId);
	}
}
