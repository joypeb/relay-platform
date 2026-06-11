package com.joypeb.gateway.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.lang.reflect.Type;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.joypeb.gateway.websocket.connection.StompConnectionRegistry;
import com.joypeb.gateway.websocket.security.StompAuthenticationChannelInterceptor;
import com.joypeb.gateway.websocket.security.StompAuthorizationException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayWebSocketIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private StompConnectionRegistry connectionRegistry;

	@Autowired
	private StompAuthenticationChannelInterceptor stompAuthenticationChannelInterceptor;

	@Test
	void connectAndSend_whenSessionCookieIsValid_receivesUserAckEvent() throws Exception {
		String sessionCookie = loginAndGetSessionCookie("stomp-user-1");
		WebSocketStompClient stompClient = stompClient();

		StompSession session = stompClient.connectAsync(
						"ws://localhost:%d/ws".formatted(port),
						webSocketHeaders(sessionCookie),
						new StompSessionHandlerAdapter() {
				})
				.get();
		await().atMost(Duration.ofSeconds(5))
				.untilAsserted(() -> assertThat(connectionRegistry.activeConnectionCount()).isEqualTo(1));

		BlockingQueue<Map<String, Object>> messages = new LinkedBlockingQueue<>();
		session.subscribe("/user/queue/gateway/acks", new MapFrameHandler(messages));
		session.send("/app/gateway/acks", Map.of(
				"requestId", "request-1",
				"type", "GATEWAY_ACK_REQUESTED",
				"payload", Map.of("source", "test"),
				"sentAt", Instant.now().toString()
		));

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(messages).isNotEmpty());
		Map<String, Object> event = messages.take();

		assertThat(event)
				.containsEntry("type", "GATEWAY_ACK_CREATED")
				.containsKeys("eventId", "occurredAt", "traceId");
		assertThat(event.get("payload"))
				.isInstanceOfSatisfying(Map.class, payload -> {
					assertThat(payload).containsEntry("requestId", "request-1");
					assertThat(payload).containsEntry("userId", "stomp-user-1");
				});

		session.disconnect();
		await().atMost(Duration.ofSeconds(5))
				.untilAsserted(() -> assertThat(connectionRegistry.activeConnectionCount()).isZero());
		stompClient.stop();
	}

	@Test
	void connect_whenSessionCookieIsMissing_failsHandshake() {
		WebSocketStompClient stompClient = stompClient();

		assertThatThrownBy(() -> stompClient.connectAsync(
						"ws://localhost:%d/ws".formatted(port),
						new StompSessionHandlerAdapter() {
						})
				.get())
				.isInstanceOf(ExecutionException.class);

		stompClient.stop();
	}

	@Test
	void stompAuthorizationAllowsChatMessageDestinations() {
		assertAllowed(StompCommand.SUBSCRIBE, "/topic/chat-rooms/11111111-1111-1111-1111-111111111111/messages");
		assertAllowed(StompCommand.SUBSCRIBE, "/user/queue/chat/acks");
		assertAllowed(StompCommand.SUBSCRIBE, "/user/queue/chat/errors");
		assertAllowed(StompCommand.SEND, "/app/chat-rooms/11111111-1111-1111-1111-111111111111/messages");
	}

	@Test
	void stompAuthorizationRejectsInvalidChatMessageDestinations() {
		assertRejected(StompCommand.SEND, "/topic/chat-rooms/11111111-1111-1111-1111-111111111111/messages");
		assertRejected(StompCommand.SUBSCRIBE, "/queue/internal");
	}

	private String loginAndGetSessionCookie(String userId) throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(java.net.URI.create("http://localhost:%d/api/v1/sessions".formatted(port)))
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("""
						{"userId":"%s"}
						""".formatted(userId)))
				.build();

		HttpResponse<String> response = HttpClient.newHttpClient()
				.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(201);
		List<String> setCookieHeaders = response.headers().allValues(HttpHeaders.SET_COOKIE);
		return setCookieHeaders.stream()
				.filter(cookie -> cookie.startsWith("JSESSIONID="))
				.findFirst()
				.orElseThrow();
	}

	private WebSocketStompClient stompClient() {
		WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
		stompClient.setMessageConverter(new JacksonJsonMessageConverter());
		return stompClient;
	}

	private void assertAllowed(StompCommand command, String destination) {
		Message<?> message = stompMessage(command, destination);
		assertThat(stompAuthenticationChannelInterceptor.preSend(message, messageChannel())).isSameAs(message);
	}

	private void assertRejected(StompCommand command, String destination) {
		assertThatThrownBy(() -> stompAuthenticationChannelInterceptor.preSend(stompMessage(command, destination), messageChannel()))
				.isInstanceOf(StompAuthorizationException.class);
	}

	private Message<?> stompMessage(StompCommand command, String destination) {
		StompHeaders headers = new StompHeaders();
		headers.setDestination(destination);
		org.springframework.messaging.simp.stomp.StompHeaderAccessor accessor =
				org.springframework.messaging.simp.stomp.StompHeaderAccessor.create(command);
		accessor.setDestination(destination);
		accessor.setUser(() -> "user-1");
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}

	private MessageChannel messageChannel() {
		return (message, timeout) -> true;
	}

	private org.springframework.web.socket.WebSocketHttpHeaders webSocketHeaders(String sessionCookie) {
		org.springframework.web.socket.WebSocketHttpHeaders headers =
				new org.springframework.web.socket.WebSocketHttpHeaders();
		headers.add(HttpHeaders.COOKIE, sessionCookie);
		return headers;
	}

	private static final class MapFrameHandler implements StompFrameHandler {

		private final BlockingQueue<Map<String, Object>> messages;

		private MapFrameHandler(BlockingQueue<Map<String, Object>> messages) {
			this.messages = messages;
		}

		@Override
		public Type getPayloadType(StompHeaders headers) {
			return Map.class;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void handleFrame(StompHeaders headers, Object payload) {
			messages.add((Map<String, Object>) payload);
		}
	}
}
