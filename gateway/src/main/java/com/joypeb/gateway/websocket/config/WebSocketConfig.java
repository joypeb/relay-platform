package com.joypeb.gateway.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import com.joypeb.gateway.websocket.handshake.GatewayHandshakeHandler;
import com.joypeb.gateway.websocket.handshake.GatewayHandshakeInterceptor;
import com.joypeb.gateway.websocket.security.StompAuthenticationChannelInterceptor;

@Configuration
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(GatewayWebSocketProperties.class)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final GatewayWebSocketProperties properties;
	private final GatewayHandshakeHandler gatewayHandshakeHandler;
	private final GatewayHandshakeInterceptor gatewayHandshakeInterceptor;
	private final StompAuthenticationChannelInterceptor stompAuthenticationChannelInterceptor;

	public WebSocketConfig(
			GatewayWebSocketProperties properties,
			GatewayHandshakeHandler gatewayHandshakeHandler,
			GatewayHandshakeInterceptor gatewayHandshakeInterceptor,
			StompAuthenticationChannelInterceptor stompAuthenticationChannelInterceptor
	) {
		this.properties = properties;
		this.gatewayHandshakeHandler = gatewayHandshakeHandler;
		this.gatewayHandshakeInterceptor = gatewayHandshakeInterceptor;
		this.stompAuthenticationChannelInterceptor = stompAuthenticationChannelInterceptor;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws")
				.setHandshakeHandler(gatewayHandshakeHandler)
				.addInterceptors(new HttpSessionHandshakeInterceptor(), gatewayHandshakeInterceptor)
				.setAllowedOriginPatterns(properties.allowedOriginPatterns().toArray(String[]::new));
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic", "/queue");
		registry.setApplicationDestinationPrefixes("/app");
		registry.setUserDestinationPrefix("/user");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(stompAuthenticationChannelInterceptor);
	}
}
