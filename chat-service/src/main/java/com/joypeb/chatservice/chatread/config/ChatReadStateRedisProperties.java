package com.joypeb.chatservice.chatread.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat.read-state.redis")
public record ChatReadStateRedisProperties(
	String roomLastSequenceKeyPattern,
	String userRoomReadSequencesKeyPattern,
	String dirtySetKey,
	int flushBatchSize
) {
	public ChatReadStateRedisProperties() {
		this(
			"chat:room:%s:last-sequence",
			"chat:user:%s:room-read-sequences",
			"chat:read-state:dirty",
			500
		);
	}

	public ChatReadStateRedisProperties {
		if (roomLastSequenceKeyPattern == null || roomLastSequenceKeyPattern.isBlank()) {
			roomLastSequenceKeyPattern = "chat:room:%s:last-sequence";
		}
		if (userRoomReadSequencesKeyPattern == null || userRoomReadSequencesKeyPattern.isBlank()) {
			userRoomReadSequencesKeyPattern = "chat:user:%s:room-read-sequences";
		}
		if (dirtySetKey == null || dirtySetKey.isBlank()) {
			dirtySetKey = "chat:read-state:dirty";
		}
		if (flushBatchSize <= 0) {
			flushBatchSize = 500;
		}
	}
}
