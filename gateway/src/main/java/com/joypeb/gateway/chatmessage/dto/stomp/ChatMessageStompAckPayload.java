package com.joypeb.gateway.chatmessage.dto.stomp;

import java.util.UUID;

public record ChatMessageStompAckPayload(
	String requestId,
	UUID messageId,
	UUID roomId,
	long sequence
) {
}
