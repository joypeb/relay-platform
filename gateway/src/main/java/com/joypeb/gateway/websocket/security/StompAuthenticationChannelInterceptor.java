package com.joypeb.gateway.websocket.security;

import java.security.Principal;
import java.util.Set;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StompAuthenticationChannelInterceptor implements ChannelInterceptor {

	private static final Set<String> ALLOWED_SUBSCRIBE_PREFIXES = Set.of(
			"/topic/",
			"/user/queue/"
	);

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
		StompCommand command = accessor.getCommand();
		if (command == null) {
			return message;
		}

		Principal user = accessor.getUser();
		if (StompCommand.CONNECT.equals(command)) {
			requireAuthenticated(user, "STOMP CONNECT requires an authenticated session.");
			return message;
		}

		if (StompCommand.SUBSCRIBE.equals(command)) {
			requireAuthenticated(user, "STOMP SUBSCRIBE requires an authenticated session.");
			requireAllowedSubscribeDestination(accessor.getDestination());
		}

		if (StompCommand.SEND.equals(command)) {
			requireAuthenticated(user, "STOMP SEND requires an authenticated session.");
			requireApplicationDestination(accessor.getDestination());
		}

		return message;
	}

	private void requireAuthenticated(Principal user, String message) {
		if (user == null || !StringUtils.hasText(user.getName())) {
			throw new StompAuthorizationException(message);
		}
	}

	private void requireAllowedSubscribeDestination(String destination) {
		if (!StringUtils.hasText(destination)) {
			throw new StompAuthorizationException("STOMP SUBSCRIBE destination is required.");
		}

		boolean allowed = ALLOWED_SUBSCRIBE_PREFIXES.stream().anyMatch(destination::startsWith);
		if (!allowed) {
			throw new StompAuthorizationException("STOMP SUBSCRIBE destination is not allowed.");
		}
	}

	private void requireApplicationDestination(String destination) {
		if (!StringUtils.hasText(destination) || !destination.startsWith("/app/")) {
			throw new StompAuthorizationException("STOMP SEND destination must start with /app/.");
		}
	}
}
