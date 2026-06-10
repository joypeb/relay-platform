package com.joypeb.gateway.config;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import com.joypeb.gateway.session.SessionAttributes;
import com.joypeb.gateway.session.UnauthenticatedException;

import jakarta.servlet.http.HttpSession;

@Configuration
@EnableConfigurationProperties(ChatServiceProperties.class)
public class ChatServiceGatewayRouteConfig {

	private static final String ACTOR_HEADER = "X-User-Id";

	@Bean
	RouterFunction<ServerResponse> chatServiceChatRoomRoutes(ChatServiceProperties properties) {
		return route("chat-service-chat-rooms")
				.route(path("/api/v1/chat-rooms").or(path("/api/v1/chat-rooms/**")), http())
				.filter(authenticatedUserHeader())
				.before(uri(properties.baseUrl()))
				.build();
	}

	private HandlerFilterFunction<ServerResponse, ServerResponse> authenticatedUserHeader() {
		return (request, next) -> {
			String userId = authenticatedUserId(request);
			ServerRequest modified = ServerRequest.from(request)
					.headers(headers -> {
						headers.remove(HttpHeaders.COOKIE);
						headers.remove(ACTOR_HEADER);
						headers.add(ACTOR_HEADER, userId);
					})
					.build();
			return next.handle(modified);
		};
	}

	private String authenticatedUserId(ServerRequest request) {
		HttpSession session = request.servletRequest().getSession(false);
		if (session == null) {
			throw new UnauthenticatedException("Authentication is required.");
		}

		Object userId = session.getAttribute(SessionAttributes.AUTHENTICATED_USER_ID);
		if (!(userId instanceof String authenticatedUserId)) {
			throw new UnauthenticatedException("Authentication is required.");
		}
		return authenticatedUserId;
	}
}
