package com.joypeb.chatservice.chatmessage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat.messages.redis")
public record ChatMessageRedisProperties(
	String streamKey
) {
	public ChatMessageRedisProperties {
		if (streamKey == null || streamKey.isBlank()) {
			streamKey = "stream:chat:message-created";
		}
	}
}
