package com.joypeb.gateway.chatmessage.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.chat-messages")
public record ChatMessageStreamProperties(
	String streamKey,
	String instanceId,
	String groupPrefix,
	String consumerName,
	int batchSize,
	Duration blockTimeout,
	boolean streamConsumerEnabled
) {
	public ChatMessageStreamProperties {
		if (streamKey == null || streamKey.isBlank()) {
			streamKey = "stream:chat:message-created";
		}
		if (instanceId == null || instanceId.isBlank()) {
			instanceId = "local";
		}
		if (groupPrefix == null || groupPrefix.isBlank()) {
			groupPrefix = "gateway-broadcast";
		}
		if (consumerName == null || consumerName.isBlank()) {
			consumerName = instanceId;
		}
		if (batchSize < 1) {
			batchSize = 20;
		}
		if (blockTimeout == null || blockTimeout.isNegative() || blockTimeout.isZero()) {
			blockTimeout = Duration.ofSeconds(2);
		}
	}

	public String groupName() {
		return groupPrefix + "-" + instanceId;
	}
}
