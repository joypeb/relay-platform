package com.joypeb.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(GatewayCorsProperties.class)
public class GatewayCorsConfig implements WebMvcConfigurer {

	private final GatewayCorsProperties properties;

	public GatewayCorsConfig(GatewayCorsProperties properties) {
		this.properties = properties;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/v1/**")
				.allowedOriginPatterns(properties.allowedOriginPatterns().toArray(String[]::new))
				.allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.exposedHeaders("Location")
				.allowCredentials(true);
	}
}
