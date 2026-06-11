package com.joypeb.chatservice.chatroom.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joypeb.chatservice.chatroom.dto.ChatRoomCreateRequest;
import com.joypeb.chatservice.chatroom.dto.ChatRoomUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class ChatRoomControllerTest {

	private static final String ACTOR_HEADER = "X-User-Id";

	@Autowired
	WebApplicationContext webApplicationContext;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	void createChatRoomReturnsCreatedRoomAndLocation() throws Exception {
		var request = new ChatRoomCreateRequest("general", "General discussion", true);

		mockMvc.perform(post("/api/v1/chat-rooms")
				.header(ACTOR_HEADER, "user-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(header().exists("Location"))
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.name").value("general"))
			.andExpect(jsonPath("$.data.description").value("General discussion"))
			.andExpect(jsonPath("$.data.ownerId").value("user-1"))
			.andExpect(jsonPath("$.data.visibility").value("PUBLIC"))
			.andExpect(jsonPath("$.data.memberCount").value(1));
	}

	@Test
	void listChatRoomsReturnsPagedPublicRooms() throws Exception {
		createRoom("user-1", "public-room", true);
		createRoom("user-2", "private-room", false);

		mockMvc.perform(get("/api/v1/chat-rooms")
				.param("scope", "public")
				.param("page", "0")
				.param("size", "20")
				.header(ACTOR_HEADER, "user-1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.items", hasSize(1)))
			.andExpect(jsonPath("$.data.items[0].name").value("public-room"))
			.andExpect(jsonPath("$.data.page").value(0))
			.andExpect(jsonPath("$.data.size").value(20))
			.andExpect(jsonPath("$.data.totalElements").value(1));
	}

	@Test
	void getChatRoomReturnsDetail() throws Exception {
		String roomId = createRoom("user-1", "detail-room", true);

		mockMvc.perform(get("/api/v1/chat-rooms/{roomId}", roomId)
				.header(ACTOR_HEADER, "user-1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(roomId))
			.andExpect(jsonPath("$.data.name").value("detail-room"))
			.andExpect(jsonPath("$.data.ownerId").value("user-1"))
			.andExpect(jsonPath("$.data.memberCount").value(1));
	}

	@Test
	void updateChatRoomRequiresOwner() throws Exception {
		String roomId = createRoom("user-1", "owned-room", true);
		var request = new ChatRoomUpdateRequest("renamed", "Updated description", false);

		mockMvc.perform(patch("/api/v1/chat-rooms/{roomId}", roomId)
				.header(ACTOR_HEADER, "user-2")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("CHAT_ROOM_FORBIDDEN"));
	}

	@Test
	void updateChatRoomReturnsUpdatedRoomForOwner() throws Exception {
		String roomId = createRoom("user-1", "old-name", true);
		var request = new ChatRoomUpdateRequest("new-name", "Updated description", false);

		mockMvc.perform(patch("/api/v1/chat-rooms/{roomId}", roomId)
				.header(ACTOR_HEADER, "user-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.name").value("new-name"))
			.andExpect(jsonPath("$.data.description").value("Updated description"))
			.andExpect(jsonPath("$.data.visibility").value("PRIVATE"));
	}

	@Test
	void inviteMemberAddsMemberToChatRoom() throws Exception {
		String roomId = createRoom("user-1", "invite-room", false);

		mockMvc.perform(post("/api/v1/chat-rooms/{roomId}/members", roomId)
				.header(ACTOR_HEADER, "user-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"memberId":"user-2"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/api/v1/chat-rooms/" + roomId + "/members/user-2"))
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.roomId").value(roomId))
			.andExpect(jsonPath("$.data.memberId").value("user-2"))
			.andExpect(jsonPath("$.data.role").value("MEMBER"));

		mockMvc.perform(get("/api/v1/chat-rooms")
				.param("scope", "joined")
				.header(ACTOR_HEADER, "user-2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items", hasSize(1)))
			.andExpect(jsonPath("$.data.items[0].id").value(roomId))
			.andExpect(jsonPath("$.data.items[0].memberCount").value(2));
	}

	@Test
	void inviteMemberRequiresOwner() throws Exception {
		String roomId = createRoom("user-1", "owner-only-invite-room", false);

		mockMvc.perform(post("/api/v1/chat-rooms/{roomId}/members", roomId)
				.header(ACTOR_HEADER, "user-2")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"memberId":"user-3"}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("CHAT_ROOM_FORBIDDEN"));
	}

	@Test
	void inviteMemberRejectsAlreadyActiveMember() throws Exception {
		String roomId = createRoom("user-1", "duplicate-invite-room", false);

		inviteMember(roomId, "user-1", "user-2");

		mockMvc.perform(post("/api/v1/chat-rooms/{roomId}/members", roomId)
				.header(ACTOR_HEADER, "user-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"memberId":"user-2"}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("CHAT_ROOM_MEMBER_ALREADY_EXISTS"));
	}

	@Test
	void deleteChatRoomHidesRoomFromDetail() throws Exception {
		String roomId = createRoom("user-1", "delete-me", true);

		mockMvc.perform(delete("/api/v1/chat-rooms/{roomId}", roomId)
				.header(ACTOR_HEADER, "user-1"))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/chat-rooms/{roomId}", roomId)
				.header(ACTOR_HEADER, "user-1"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("CHAT_ROOM_NOT_FOUND"));
	}

	private String createRoom(String ownerId, String name, boolean publiclyVisible) throws Exception {
		var request = new ChatRoomCreateRequest(name, "Description for " + name, publiclyVisible);
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
}
