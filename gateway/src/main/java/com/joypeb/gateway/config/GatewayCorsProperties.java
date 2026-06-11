package com.joypeb.gateway.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.cors")
public record GatewayCorsProperties(
		List<String> allowedOriginPatterns
) {

	public GatewayCorsProperties {
		if (allowedOriginPatterns == null || allowedOriginPatterns.isEmpty()) {
			allowedOriginPatterns = List.of("http://localhost:*", "http://127.0.0.1:*");
		}
	}
}
