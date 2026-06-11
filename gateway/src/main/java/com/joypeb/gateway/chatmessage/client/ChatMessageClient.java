package com.joypeb.gateway.chatmessage.client;

import java.util.UUID;

public interface ChatMessageClient {

	SentMessage send(String senderId, UUID roomId, String requestId, String type, String content);

	record SentMessage(
		UUID messageId,
		UUID roomId,
		long sequence
	) {
	}
}
