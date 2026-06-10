package com.joypeb.gateway.websocket.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.websocket")
public record GatewayWebSocketProperties(
		List<String> allowedOriginPatterns
) {

	public GatewayWebSocketProperties {
		if (allowedOriginPatterns == null || allowedOriginPatterns.isEmpty()) {
			allowedOriginPatterns = List.of("*");
		}
	}
}
