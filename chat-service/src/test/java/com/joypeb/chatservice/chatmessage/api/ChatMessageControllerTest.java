package com.joypeb.chatservice.chatmessage.api;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joypeb.chatservice.chatmessage.application.ChatMessagePublisher;
import com.joypeb.chatservice.chatmessage.event.ChatMessageCreatedEvent;
import com.joypeb.chatservice.chatroom.dto.ChatRoomCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class ChatMessageControllerTest {

	private static final String ACTOR_HEADER = "X-User-Id";

	@Autowired
	WebApplicationContext webApplicationContext;

	@MockitoBean
	ChatMessagePublisher chatMessagePublisher;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		doNothing().when(chatMessagePublisher).publishCreated(any(ChatMessageCreatedEvent.class));
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	void sendMessageStoresMessageWithRoomSequence() throws Exception {
		String roomId = createRoom("user-1", "message-room");
		inviteMember(roomId, "user-1", "user-2");

		mockMvc.perform(post("/internal/chat-rooms/{roomId}/messages", roomId)
				.header(ACTOR_HEADER, "user-2")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"requestId":"req-1","type":"TEXT","content":"hello"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.roomId").value(roomId))
			.andExpect(jsonPath("$.data.senderId").value("user-2"))
			.andExpect(jsonPath("$.data.sequence").value(1))
			.andExpect(jsonPath("$.data.type").value("TEXT"))
			.andExpect(jsonPath("$.data.content").value("hello"));

		mockMvc.perform(post("/internal/chat-rooms/{roomId}/messages", roomId)
				.header(ACTOR_HEADER, "user-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"requestId":"req-2","type":"TEXT","content":"second"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.sequence").value(2));
	}

	@Test
	void sendMessageRequiresMembership() throws Exception {
		String roomId = createRoom("user-1", "private-message-room");

		mockMvc.perform(post("/internal/chat-rooms/{roomId}/messages", roomId)
				.header(ACTOR_HEADER, "user-3")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"requestId":"req-1","type":"TEXT","content":"blocked"}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("CHAT_ROOM_FORBIDDEN"));
	}

	@Test
	void historyReturnsMessagesAfterSequence() throws Exception {
		String roomId = createRoom("user-1", "history-room");

		sendMessage(roomId, "user-1", "req-1", "first");
		sendMessage(roomId, "user-1", "req-2", "second");

		mockMvc.perform(get("/api/v1/chat-rooms/{roomId}/messages", roomId)
				.header(ACTOR_HEADER, "user-1")
				.param("afterSequence", "1")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data", hasSize(1)))
			.andExpect(jsonPath("$.data[0].sequence").value(2))
			.andExpect(jsonPath("$.data[0].content").value("second"));
	}

	private String createRoom(String ownerId, String name) throws Exception {
		var request = new ChatRoomCreateRequest(name, "Description for " + name, false);
		String response = mockMvc.perform(post("/api/v1/chat-rooms")
				.header(ACTOR_HEADER, ownerId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return objectMapper.readTree(response).path("data").path("id").asText();
	}

	private void inviteMember(String roomId, String ownerId, String memberId) throws Exception {
		mockMvc.perform(post("/api/v1/chat-rooms/{roomId}/members", roomId)
				.header(ACTOR_HEADER, ownerId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"memberId":"%s"}
					""".formatted(memberId)))
			.andExpect(status().isCreated());
	}

	private void sendMessage(String roomId, String senderId, String requestId, String content) throws Exception {
		mockMvc.perform(post("/internal/chat-rooms/{roomId}/messages", roomId)
				.header(ACTOR_HEADER, senderId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"requestId":"%s","type":"TEXT","content":"%s"}
					""".formatted(requestId, content)))
			.andExpect(status().isCreated());
	}
}
