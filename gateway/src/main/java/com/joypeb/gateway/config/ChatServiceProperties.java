package com.joypeb.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.chat-service")
public record ChatServiceProperties(
		String baseUrl
) {
}
