package com.joypeb.gateway.api.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class ChatRoomGatewayRouteTest {

	private static HttpServer chatService;
	private static final AtomicReference<String> forwardedUserId = new AtomicReference<>();
	private static final AtomicReference<String> forwardedCookie = new AtomicReference<>();
	private static final AtomicReference<String> forwardedBody = new AtomicReference<>();

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;

	@BeforeAll
	static void startChatService() throws IOException {
		chatService = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		chatService.createContext("/api/v1/chat-rooms", ChatRoomGatewayRouteTest::handleCreateChatRoom);
		chatService.start();
	}

	@AfterAll
	static void stopChatService() {
		chatService.stop(0);
	}

	@DynamicPropertySource
	static void chatServiceProperties(DynamicPropertyRegistry registry) {
		registry.add("gateway.chat-service.base-url", () -> "http://localhost:" + chatService.getAddress().getPort());
	}

	@BeforeEach
	void setUp() {
		forwardedUserId.set(null);
		forwardedCookie.set(null);
		forwardedBody.set(null);
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	void createChatRoom_whenSessionExists_routesThroughSpringCloudGatewayMvc() throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/api/v1/sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":"user-1"}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		mockMvc.perform(post("/api/v1/chat-rooms")
						.session(session)
						.header("X-User-Id", "spoofed-user")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"general","description":"General discussion","publiclyVisible":true}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/chat-rooms/11111111-1111-1111-1111-111111111111"))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.name").value("general"));

		assertThat(forwardedUserId.get()).isEqualTo("user-1");
		assertThat(forwardedCookie.get()).isNull();
		assertThat(forwardedBody.get()).contains("\"name\":\"general\"");
	}

	@Test
	void createChatRoom_whenSessionIsMissing_returnsUnauthorizedProblem() throws Exception {
		mockMvc.perform(post("/api/v1/chat-rooms")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"general","description":"General discussion","publiclyVisible":true}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	private static void handleCreateChatRoom(HttpExchange exchange) throws IOException {
		forwardedUserId.set(exchange.getRequestHeaders().getFirst("X-User-Id"));
		forwardedCookie.set(exchange.getRequestHeaders().getFirst(HttpHeaders.COOKIE));
		forwardedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

		byte[] response = """
				{
				  "success": true,
				  "data": {
				    "id": "11111111-1111-1111-1111-111111111111",
				    "name": "general",
				    "description": "General discussion",
				    "ownerId": "user-1",
				    "publiclyVisible": true,
				    "memberCount": 1
				  },
				  "traceId": null,
				  "timestamp": "2026-06-10T00:00:00Z"
				}
				""".getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		exchange.getResponseHeaders().add(HttpHeaders.LOCATION, "/api/v1/chat-rooms/11111111-1111-1111-1111-111111111111");
		exchange.sendResponseHeaders(201, response.length);
		exchange.getResponseBody().write(response);
		exchange.close();
	}
}
