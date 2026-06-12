package com.joypeb.chatservice.chatread.dto;

import java.util.UUID;

public record ChatReadReceiptResponse(
	String requestId,
	UUID roomId,
	long lastReadSequence
) {
}
