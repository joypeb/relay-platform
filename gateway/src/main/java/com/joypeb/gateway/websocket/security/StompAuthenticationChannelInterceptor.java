package com.joypeb.gateway.websocket.security;

import java.security.Principal;
import java.util.regex.Pattern;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StompAuthenticationChannelInterceptor implements ChannelInterceptor {

	private static final Pattern CHAT_ROOM_MESSAGES_TOPIC = Pattern.compile(
			"^/topic/chat-rooms/[0-9a-fA-F-]{36}/messages$"
	);

	private static final Pattern CHAT_ROOM_MESSAGES_SEND = Pattern.compile(
			"^/app/chat-rooms/[0-9a-fA-F-]{36}/messages$"
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

		boolean allowed = destination.equals("/user/queue/chat/acks")
				|| destination.equals("/user/queue/chat/errors")
				|| destination.equals("/user/queue/gateway/acks")
				|| CHAT_ROOM_MESSAGES_TOPIC.matcher(destination).matches();
		if (!allowed) {
			throw new StompAuthorizationException("STOMP SUBSCRIBE destination is not allowed.");
		}
	}

	private void requireApplicationDestination(String destination) {
		if (!StringUtils.hasText(destination)) {
			throw new StompAuthorizationException("STOMP SEND destination is required.");
		}

		boolean allowed = destination.equals("/app/gateway/acks")
				|| CHAT_ROOM_MESSAGES_SEND.matcher(destination).matches();
		if (!allowed) {
			throw new StompAuthorizationException("STOMP SEND destination is not allowed.");
		}
	}
}
