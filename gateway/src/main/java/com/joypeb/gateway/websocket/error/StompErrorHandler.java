package com.joypeb.gateway.websocket.error;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import com.joypeb.gateway.websocket.security.StompAuthorizationException;

@Configuration
public class StompErrorHandler extends StompSubProtocolErrorHandler {

	@Override
	public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
		Throwable cause = ex instanceof MessageDeliveryException messageDeliveryException
				&& messageDeliveryException.getCause() != null
				? messageDeliveryException.getCause()
				: ex;

		if (cause instanceof StompAuthorizationException) {
			StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
			accessor.setMessage(cause.getMessage());
			accessor.setLeaveMutable(true);
			return MessageBuilder.createMessage(cause.getMessage().getBytes(), accessor.getMessageHeaders());
		}

		return super.handleClientMessageProcessingError(clientMessage, ex);
	}
}
