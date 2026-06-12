package com.joypeb.gateway.chatmessage.dto.stomp;

import java.util.UUID;

public record ChatReadReceiptStompAckPayload(
	String requestId,
	UUID roomId,
	long lastReadSequence
) {
}
