package com.joypeb.gateway.websocket.handshake;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import com.joypeb.gateway.session.GatewayUserPrincipal;
import com.joypeb.gateway.session.SessionAttributes;

@Component
public class GatewayHandshakeHandler extends DefaultHandshakeHandler {

	@Override
	protected Principal determineUser(
			ServerHttpRequest request,
			WebSocketHandler wsHandler,
			Map<String, Object> attributes
	) {
		Object userId = attributes.get(SessionAttributes.AUTHENTICATED_USER_ID);
		if (userId instanceof String authenticatedUserId) {
			return new GatewayUserPrincipal(authenticatedUserId);
		}
		return super.determineUser(request, wsHandler, attributes);
	}
}
