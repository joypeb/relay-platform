package com.joypeb.gateway.chatmessage.client;

import java.util.UUID;

public interface ChatMessageClient {

	SentMessage send(String senderId, UUID roomId, String requestId, String type, String content);

	AcceptedReadReceipt readReceipt(String actorId, UUID roomId, String requestId, String type, long lastReadSequence);

	record SentMessage(
		UUID messageId,
		UUID roomId,
		long sequence
	) {
	}

	record AcceptedReadReceipt(
		UUID roomId,
		long lastReadSequence
	) {
	}
}
