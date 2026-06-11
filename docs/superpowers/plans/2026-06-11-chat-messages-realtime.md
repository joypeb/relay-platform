# Chat Messages Realtime Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build room-scoped chat message sending, durable room sequence storage, Redis Stream publication, multi-gateway stream consumption, STOMP broadcast, and REST history recovery.

**Architecture:** `chat-service` owns chat message persistence, room membership authorization, room-local sequence assignment, and Redis Stream event publication. `gateway` owns authenticated STOMP ingress, forwarding send commands to `chat-service`, per-instance Redis Stream consumption, and STOMP broadcast to clients connected to that gateway. PostgreSQL is the message source of truth; Redis Streams are delivery events for realtime fan-out; REST history is the recovery path after reconnect.

**Tech Stack:** Java 21, Spring Boot 4, Spring Web MVC, Spring WebSocket/STOMP, Spring Data JPA, PostgreSQL/Flyway, Redis Streams via Spring Data Redis, MockMvc, Spring WebSocket STOMP tests.

---

## Scope And Contracts

Implement the recommended multi-gateway design:

1. Client sends STOMP command to gateway: `SEND /app/chat-rooms/{roomId}/messages`.
2. Gateway validates authenticated principal and forwards to `chat-service`: `POST /internal/chat-rooms/{roomId}/messages`.
3. `chat-service` verifies active room membership, assigns a room-local `sequence`, stores `chat_messages`, and publishes a Redis Stream event after DB commit.
4. Every gateway instance reads the same `stream:chat:message-created` event through its own consumer group.
5. Each gateway broadcasts the event to local WebSocket clients subscribed to `/topic/chat-rooms/{roomId}/messages`.
6. Clients recover missed messages through `GET /api/v1/chat-rooms/{roomId}/messages?afterSequence={sequence}&size={size}`.

Do not use one shared gateway consumer group such as `gateway-group` for broadcast. A single group distributes each stream entry to only one gateway instance. Use a group name that includes the gateway instance id, for example `gateway-broadcast-${instanceId}`.

## File Structure

### chat-service files

- Create: `chat-service/src/main/resources/db/migration/V2__create_chat_messages.sql`
  - Defines `chat_message_sequences` and `chat_messages`.
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/domain/ChatMessage.java`
  - JPA entity for persisted room messages.
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/domain/ChatMessageType.java`
  - Enum for message type. Start with `TEXT`.
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/domain/ChatMessageSequence.java`
  - JPA entity that owns the next sequence value for one room.
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/infrastructure/ChatMessageRepository.java`
  - Saves messages and queries room history.
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/infrastructure/ChatMessageSequenceRepository.java`
  - Finds room sequence row with pessimistic write lock.
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/application/ChatMessageService.java`
  - Transaction boundary for send and read-history use cases.
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/application/ChatMessagePublisher.java`
  - Port used by application service after commit.
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/infrastructure/RedisChatMessagePublisher.java`
  - Redis Stream publisher implementation.
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/config/ChatMessageRedisProperties.java`
  - Externalized stream key and publisher settings.
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/dto/ChatMessageSendRequest.java`
  - Internal REST request DTO.
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/dto/ChatMessageResponse.java`
  - REST response DTO.
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/event/ChatMessageCreatedEvent.java`
  - Redis Stream payload.
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/api/ChatMessageController.java`
  - Internal send endpoint and public history endpoint.
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/application/ChatMessageContentInvalidException.java`
  - Domain validation failure for invalid content.
- Create: `chat-service/src/test/java/com/joypeb/chatservice/chatmessage/api/ChatMessageControllerTest.java`
  - MockMvc integration tests.
- Modify: `chat-service/build.gradle`
  - Add Spring Data Redis dependency.
- Modify: `chat-service/src/main/resources/application.yaml`
  - Add Redis and chat message stream settings.
- Modify: `chat-service/src/test/resources/application.yaml`
  - Add test Redis settings if tests mock the publisher, or disable publisher for controller tests.
- Modify: `chat-service/src/main/java/com/joypeb/chatservice/chatroom/infrastructure/ChatRoomMemberRepository.java`
  - Add membership check query.
- Modify: `chat-service/src/main/java/com/joypeb/chatservice/common/error/ChatServiceExceptionHandler.java`
  - Map message validation and forbidden/not-found errors consistently.

### gateway files

- Create: `gateway/src/main/java/com/joypeb/gateway/chatmessage/api/stomp/ChatMessageStompController.java`
  - Handles `SEND /app/chat-rooms/{roomId}/messages`.
- Create: `gateway/src/main/java/com/joypeb/gateway/chatmessage/client/ChatMessageClient.java`
  - Interface for forwarding message send commands.
- Create: `gateway/src/main/java/com/joypeb/gateway/chatmessage/client/RestChatMessageClient.java`
  - Calls `chat-service` internal REST endpoint.
- Create: `gateway/src/main/java/com/joypeb/gateway/chatmessage/dto/stomp/ChatMessageStompSendRequest.java`
  - STOMP request envelope.
- Create: `gateway/src/main/java/com/joypeb/gateway/chatmessage/dto/stomp/ChatMessageStompAckPayload.java`
  - User-specific ACK payload.
- Create: `gateway/src/main/java/com/joypeb/gateway/chatmessage/dto/stomp/ChatMessageStompErrorPayload.java`
  - User-specific error payload.
- Create: `gateway/src/main/java/com/joypeb/gateway/chatmessage/dto/stomp/ChatMessageBroadcastPayload.java`
  - Broadcast payload.
- Create: `gateway/src/main/java/com/joypeb/gateway/chatmessage/event/ChatMessageCreatedStreamEvent.java`
  - Redis Stream event DTO matching `chat-service`.
- Create: `gateway/src/main/java/com/joypeb/gateway/chatmessage/stream/ChatMessageStreamConsumer.java`
  - Reads Redis Stream with a gateway-instance-specific consumer group and broadcasts messages.
- Create: `gateway/src/main/java/com/joypeb/gateway/chatmessage/config/ChatMessageStreamProperties.java`
  - Externalizes stream key, instance id, group name prefix, consumer name, block timeout, and batch size.
- Create: `gateway/src/test/java/com/joypeb/gateway/chatmessage/api/stomp/ChatMessageStompControllerTest.java`
  - Tests STOMP send forwarding and ACK/error routing.
- Create: `gateway/src/test/java/com/joypeb/gateway/chatmessage/stream/ChatMessageStreamConsumerTest.java`
  - Tests stream event to STOMP broadcast mapping.
- Modify: `gateway/build.gradle`
  - Add Spring Data Redis and web client dependency if needed.
- Modify: `gateway/src/main/resources/application.yaml`
  - Add chat message stream and instance id settings.
- Modify: `gateway/src/main/java/com/joypeb/gateway/websocket/security/StompAuthenticationChannelInterceptor.java`
  - Restrict chat message destinations and block invalid send/subscribe paths.
- Modify: `gateway/src/main/java/com/joypeb/gateway/config/ChatServiceProperties.java`
  - Reuse existing base URL for internal message forwarding.

### Documentation files

- Modify: `docs/architecture/overview.md`
  - Document Redis Stream fan-out with per-gateway consumer groups.
- Create: `docs/services/chat-service/features/chat-messages.md`
  - Document message persistence, sequence, Redis Stream contract, REST history, failures, and tests.
- Modify: `docs/services/gateway/features/local-session-and-stomp-gateway.md`
  - Add message send, ACK/error queues, stream consumer, and broadcast contract.

---

## Task 1: Add Chat Message Schema

**Files:**
- Create: `chat-service/src/main/resources/db/migration/V2__create_chat_messages.sql`

- [ ] **Step 1: Write the migration**

Create `V2__create_chat_messages.sql`:

```sql
create table chat_message_sequences (
    room_id uuid primary key,
    next_sequence bigint not null,
    constraint fk_chat_message_sequences_room_id foreign key (room_id) references chat_rooms (id),
    constraint ck_chat_message_sequences_next_sequence check (next_sequence >= 1)
);

create table chat_messages (
    id uuid primary key,
    room_id uuid not null,
    sender_id varchar(64) not null,
    sequence bigint not null,
    type varchar(20) not null,
    content varchar(2000) not null,
    created_at timestamp with time zone not null,
    deleted_at timestamp with time zone,
    constraint fk_chat_messages_room_id foreign key (room_id) references chat_rooms (id),
    constraint ck_chat_messages_type check (type in ('TEXT')),
    constraint ck_chat_messages_sequence check (sequence >= 1),
    constraint ck_chat_messages_content_not_blank check (length(trim(content)) > 0)
);

create unique index uk_chat_messages_room_id_sequence on chat_messages (room_id, sequence);
create index idx_chat_messages_room_id_created_at on chat_messages (room_id, created_at);
create index idx_chat_messages_room_id_sequence on chat_messages (room_id, sequence);
create index idx_chat_messages_sender_id on chat_messages (sender_id);
```

- [ ] **Step 2: Run chat-service tests to verify migration loads**

Run:

```bash
cd chat-service
./gradlew test
```

Expected: existing tests pass and application context loads the new migration.

- [ ] **Step 3: Commit schema**

```bash
git add chat-service/src/main/resources/db/migration/V2__create_chat_messages.sql
git commit -m "[DB] Add chat message schema"
```

---

## Task 2: Add Chat Message Domain And Repositories

**Files:**
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/domain/ChatMessageType.java`
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/domain/ChatMessage.java`
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/domain/ChatMessageSequence.java`
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/infrastructure/ChatMessageRepository.java`
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/infrastructure/ChatMessageSequenceRepository.java`
- Modify: `chat-service/src/main/java/com/joypeb/chatservice/chatroom/infrastructure/ChatRoomMemberRepository.java`

- [ ] **Step 1: Add message type enum**

Create `ChatMessageType.java`:

```java
package com.joypeb.chatservice.chatmessage.domain;

public enum ChatMessageType {
	TEXT
}
```

- [ ] **Step 2: Add message entity**

Create `ChatMessage.java`:

```java
package com.joypeb.chatservice.chatmessage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "room_id", nullable = false)
	private UUID roomId;

	@Column(name = "sender_id", nullable = false, length = 64)
	private String senderId;

	@Column(name = "sequence", nullable = false)
	private long sequence;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private ChatMessageType type;

	@Column(name = "content", nullable = false, length = 2000)
	private String content;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	protected ChatMessage() {
	}

	private ChatMessage(UUID roomId, String senderId, long sequence, ChatMessageType type, String content, Instant createdAt) {
		this.id = UUID.randomUUID();
		this.roomId = roomId;
		this.senderId = senderId;
		this.sequence = sequence;
		this.type = type;
		this.content = content;
		this.createdAt = createdAt;
	}

	public static ChatMessage text(UUID roomId, String senderId, long sequence, String content, Instant createdAt) {
		return new ChatMessage(roomId, senderId, sequence, ChatMessageType.TEXT, content, createdAt);
	}

	public UUID getId() {
		return id;
	}

	public UUID getRoomId() {
		return roomId;
	}

	public String getSenderId() {
		return senderId;
	}

	public long getSequence() {
		return sequence;
	}

	public ChatMessageType getType() {
		return type;
	}

	public String getContent() {
		return content;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
```

- [ ] **Step 3: Add room sequence entity**

Create `ChatMessageSequence.java`:

```java
package com.joypeb.chatservice.chatmessage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "chat_message_sequences")
public class ChatMessageSequence {

	@Id
	@Column(name = "room_id", nullable = false)
	private UUID roomId;

	@Column(name = "next_sequence", nullable = false)
	private long nextSequence;

	protected ChatMessageSequence() {
	}

	private ChatMessageSequence(UUID roomId) {
		this.roomId = roomId;
		this.nextSequence = 1;
	}

	public static ChatMessageSequence create(UUID roomId) {
		return new ChatMessageSequence(roomId);
	}

	public long issue() {
		long issued = nextSequence;
		nextSequence += 1;
		return issued;
	}

	public UUID getRoomId() {
		return roomId;
	}
}
```

- [ ] **Step 4: Add repositories**

Create `ChatMessageRepository.java`:

```java
package com.joypeb.chatservice.chatmessage.infrastructure;

import com.joypeb.chatservice.chatmessage.domain.ChatMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

	List<ChatMessage> findByRoomIdAndSequenceGreaterThanAndDeletedAtIsNullOrderBySequenceAsc(
		UUID roomId,
		long sequence,
		Pageable pageable
	);
}
```

Create `ChatMessageSequenceRepository.java`:

```java
package com.joypeb.chatservice.chatmessage.infrastructure;

import com.joypeb.chatservice.chatmessage.domain.ChatMessageSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ChatMessageSequenceRepository extends JpaRepository<ChatMessageSequence, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ChatMessageSequence> findWithLockByRoomId(UUID roomId);
}
```

- [ ] **Step 5: Add membership repository query**

Modify `ChatRoomMemberRepository.java`:

```java
package com.joypeb.chatservice.chatroom.infrastructure;

import com.joypeb.chatservice.chatroom.domain.ChatRoomMember;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, UUID> {

	long countByRoomIdAndLeftAtIsNull(UUID roomId);

	boolean existsByRoomIdAndMemberIdAndLeftAtIsNull(UUID roomId, String memberId);
}
```

This file already contains the required membership method. Keep it unchanged if it matches this content.

- [ ] **Step 6: Run tests**

Run:

```bash
cd chat-service
./gradlew test
```

Expected: all tests pass.

- [ ] **Step 7: Commit domain and repositories**

```bash
git add chat-service/src/main/java/com/joypeb/chatservice/chatmessage chat-service/src/main/java/com/joypeb/chatservice/chatroom/infrastructure/ChatRoomMemberRepository.java
git commit -m "[FEAT] Add chat message domain"
```

---

## Task 3: Implement Chat-Service Message Send And History

**Files:**
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/dto/ChatMessageSendRequest.java`
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/dto/ChatMessageResponse.java`
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/application/ChatMessageContentInvalidException.java`
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/application/ChatMessageService.java`
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/api/ChatMessageController.java`
- Modify: `chat-service/src/main/java/com/joypeb/chatservice/common/error/ChatServiceExceptionHandler.java`
- Test: `chat-service/src/test/java/com/joypeb/chatservice/chatmessage/api/ChatMessageControllerTest.java`

- [ ] **Step 1: Write failing controller tests**

Create `ChatMessageControllerTest.java` with these tests:

```java
package com.joypeb.chatservice.chatmessage.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joypeb.chatservice.chatroom.dto.ChatRoomCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class ChatMessageControllerTest {

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
```

- [ ] **Step 2: Run failing test**

Run:

```bash
cd chat-service
./gradlew test --tests '*ChatMessageControllerTest'
```

Expected: FAIL because message controller and DTOs do not exist.

- [ ] **Step 3: Add DTOs**

Create `ChatMessageSendRequest.java`:

```java
package com.joypeb.chatservice.chatmessage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatMessageSendRequest(
	@NotBlank
	@Size(max = 64)
	String requestId,

	@NotBlank
	@Pattern(regexp = "TEXT")
	String type,

	@NotBlank
	@Size(max = 2000)
	String content
) {
}
```

Create `ChatMessageResponse.java`:

```java
package com.joypeb.chatservice.chatmessage.dto;

import com.joypeb.chatservice.chatmessage.domain.ChatMessageType;
import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
	UUID id,
	UUID roomId,
	String senderId,
	long sequence,
	ChatMessageType type,
	String content,
	Instant createdAt
) {
}
```

- [ ] **Step 4: Add application exception**

Create `ChatMessageContentInvalidException.java`:

```java
package com.joypeb.chatservice.chatmessage.application;

public class ChatMessageContentInvalidException extends RuntimeException {

	public ChatMessageContentInvalidException() {
		super("Chat message content is invalid.");
	}
}
```

- [ ] **Step 5: Add service**

Create `ChatMessageService.java`:

```java
package com.joypeb.chatservice.chatmessage.application;

import com.joypeb.chatservice.chatmessage.domain.ChatMessage;
import com.joypeb.chatservice.chatmessage.domain.ChatMessageSequence;
import com.joypeb.chatservice.chatmessage.dto.ChatMessageResponse;
import com.joypeb.chatservice.chatmessage.dto.ChatMessageSendRequest;
import com.joypeb.chatservice.chatmessage.infrastructure.ChatMessageRepository;
import com.joypeb.chatservice.chatmessage.infrastructure.ChatMessageSequenceRepository;
import com.joypeb.chatservice.chatroom.application.ChatRoomForbiddenException;
import com.joypeb.chatservice.chatroom.application.ChatRoomNotFoundException;
import com.joypeb.chatservice.chatroom.infrastructure.ChatRoomMemberRepository;
import com.joypeb.chatservice.chatroom.infrastructure.ChatRoomRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatMessageService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final ChatMessageSequenceRepository chatMessageSequenceRepository;
	private final Clock clock;

	public ChatMessageService(
		ChatRoomRepository chatRoomRepository,
		ChatRoomMemberRepository chatRoomMemberRepository,
		ChatMessageRepository chatMessageRepository,
		ChatMessageSequenceRepository chatMessageSequenceRepository,
		Clock clock
	) {
		this.chatRoomRepository = chatRoomRepository;
		this.chatRoomMemberRepository = chatRoomMemberRepository;
		this.chatMessageRepository = chatMessageRepository;
		this.chatMessageSequenceRepository = chatMessageSequenceRepository;
		this.clock = clock;
	}

	@Transactional
	public ChatMessageResponse send(String senderId, UUID roomId, ChatMessageSendRequest request) {
		requireActiveRoom(roomId);
		requireMember(roomId, senderId);
		String content = normalizeContent(request.content());
		ChatMessageSequence sequence = chatMessageSequenceRepository.findWithLockByRoomId(roomId)
			.orElseGet(() -> chatMessageSequenceRepository.saveAndFlush(ChatMessageSequence.create(roomId)));
		Instant now = clock.instant();
		ChatMessage message = ChatMessage.text(roomId, senderId, sequence.issue(), content, now);
		return toResponse(chatMessageRepository.save(message));
	}

	@Transactional(readOnly = true)
	public List<ChatMessageResponse> history(String actorId, UUID roomId, long afterSequence, int size) {
		requireActiveRoom(roomId);
		requireMember(roomId, actorId);
		int boundedSize = Math.min(Math.max(size, 1), 100);
		return chatMessageRepository
			.findByRoomIdAndSequenceGreaterThanAndDeletedAtIsNullOrderBySequenceAsc(
				roomId,
				afterSequence,
				PageRequest.of(0, boundedSize)
			)
			.stream()
			.map(this::toResponse)
			.toList();
	}

	private void requireActiveRoom(UUID roomId) {
		if (chatRoomRepository.findByIdAndDeletedAtIsNull(roomId).isEmpty()) {
			throw new ChatRoomNotFoundException(roomId);
		}
	}

	private void requireMember(UUID roomId, String actorId) {
		if (!chatRoomMemberRepository.existsByRoomIdAndMemberIdAndLeftAtIsNull(roomId, actorId)) {
			throw new ChatRoomForbiddenException();
		}
	}

	private String normalizeContent(String content) {
		String normalized = content == null ? "" : content.trim();
		if (normalized.isBlank() || normalized.length() > 2000) {
			throw new ChatMessageContentInvalidException();
		}
		return normalized;
	}

	private ChatMessageResponse toResponse(ChatMessage message) {
		return new ChatMessageResponse(
			message.getId(),
			message.getRoomId(),
			message.getSenderId(),
			message.getSequence(),
			message.getType(),
			message.getContent(),
			message.getCreatedAt()
		);
	}
}
```

- [ ] **Step 6: Add controller**

Create `ChatMessageController.java`:

```java
package com.joypeb.chatservice.chatmessage.api;

import com.joypeb.chatservice.chatmessage.application.ChatMessageService;
import com.joypeb.chatservice.chatmessage.dto.ChatMessageResponse;
import com.joypeb.chatservice.chatmessage.dto.ChatMessageSendRequest;
import com.joypeb.chatservice.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping
public class ChatMessageController {

	private static final String ACTOR_HEADER = "X-User-Id";

	private final ChatMessageService chatMessageService;

	public ChatMessageController(ChatMessageService chatMessageService) {
		this.chatMessageService = chatMessageService;
	}

	@PostMapping("/internal/chat-rooms/{roomId}/messages")
	public ResponseEntity<ApiResponse<ChatMessageResponse>> send(
		@RequestHeader(ACTOR_HEADER) @Pattern(regexp = "^[A-Za-z0-9._-]{1,64}$") String actorId,
		@PathVariable UUID roomId,
		@Valid @RequestBody ChatMessageSendRequest request
	) {
		ChatMessageResponse response = chatMessageService.send(actorId, roomId, request);
		return ResponseEntity
			.created(URI.create("/api/v1/chat-rooms/" + roomId + "/messages/" + response.id()))
			.body(ApiResponse.success(response));
	}

	@GetMapping("/api/v1/chat-rooms/{roomId}/messages")
	public ApiResponse<List<ChatMessageResponse>> history(
		@RequestHeader(ACTOR_HEADER) @Pattern(regexp = "^[A-Za-z0-9._-]{1,64}$") String actorId,
		@PathVariable UUID roomId,
		@RequestParam(defaultValue = "0") @Min(0) long afterSequence,
		@RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
	) {
		return ApiResponse.success(chatMessageService.history(actorId, roomId, afterSequence, size));
	}
}
```

- [ ] **Step 7: Map message validation exception**

Modify `ChatServiceExceptionHandler.java` to include `ChatMessageContentInvalidException` in bad request handling:

```java
import com.joypeb.chatservice.chatmessage.application.ChatMessageContentInvalidException;
```

Then add a handler:

```java
@ExceptionHandler(ChatMessageContentInvalidException.class)
public ResponseEntity<ProblemDetail> handleInvalidMessageContent(
	ChatMessageContentInvalidException exception,
	HttpServletRequest request
) {
	ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Chat message content invalid", exception.getMessage(), request);
	problem.setType(URI.create("https://joypeb.com/problems/chat-message-content-invalid"));
	problem.setProperty("code", "CHAT_MESSAGE_CONTENT_INVALID");
	return ResponseEntity.badRequest().body(problem);
}
```

- [ ] **Step 8: Run tests**

Run:

```bash
cd chat-service
./gradlew test --tests '*ChatMessageControllerTest'
./gradlew test
```

Expected: all chat-service tests pass.

- [ ] **Step 9: Commit send and history**

```bash
git add chat-service/src/main/java/com/joypeb/chatservice/chatmessage chat-service/src/test/java/com/joypeb/chatservice/chatmessage chat-service/src/main/java/com/joypeb/chatservice/common/error/ChatServiceExceptionHandler.java
git commit -m "[FEAT] Add chat message send and history"
```

---

## Task 4: Publish Chat Message Created Events To Redis Stream

**Files:**
- Modify: `chat-service/build.gradle`
- Modify: `chat-service/src/main/resources/application.yaml`
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/event/ChatMessageCreatedEvent.java`
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/application/ChatMessagePublisher.java`
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/config/ChatMessageRedisProperties.java`
- Create: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/infrastructure/RedisChatMessagePublisher.java`
- Modify: `chat-service/src/main/java/com/joypeb/chatservice/chatmessage/application/ChatMessageService.java`
- Test: `chat-service/src/test/java/com/joypeb/chatservice/chatmessage/application/ChatMessageServiceTest.java`

- [ ] **Step 1: Add Redis dependency**

Modify `chat-service/build.gradle` dependencies:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

- [ ] **Step 2: Add configuration**

Modify `chat-service/src/main/resources/application.yaml`:

```yaml
spring:
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:localhost}
      port: ${SPRING_DATA_REDIS_PORT:6379}

chat:
  messages:
    redis:
      stream-key: ${CHAT_MESSAGE_STREAM_KEY:stream:chat:message-created}
```

Keep existing YAML keys intact and merge these keys under existing `spring` if it already exists.

- [ ] **Step 3: Add stream event DTO**

Create `ChatMessageCreatedEvent.java`:

```java
package com.joypeb.chatservice.chatmessage.event;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageCreatedEvent(
	String eventId,
	String eventType,
	UUID aggregateId,
	Instant occurredAt,
	int schemaVersion,
	Payload payload,
	String traceId
) {
	public static ChatMessageCreatedEvent from(
		String eventId,
		UUID messageId,
		UUID roomId,
		String senderId,
		long sequence,
		String type,
		String content,
		Instant createdAt,
		Instant occurredAt,
		String traceId
	) {
		return new ChatMessageCreatedEvent(
			eventId,
			"CHAT_MESSAGE_CREATED",
			roomId,
			occurredAt,
			1,
			new Payload(messageId, roomId, senderId, sequence, type, content, createdAt),
			traceId
		);
	}

	public record Payload(
		UUID messageId,
		UUID roomId,
		String senderId,
		long sequence,
		String type,
		String content,
		Instant createdAt
	) {
	}
}
```

- [ ] **Step 4: Add publisher port and properties**

Create `ChatMessagePublisher.java`:

```java
package com.joypeb.chatservice.chatmessage.application;

import com.joypeb.chatservice.chatmessage.event.ChatMessageCreatedEvent;

public interface ChatMessagePublisher {

	void publishCreated(ChatMessageCreatedEvent event);
}
```

Create `ChatMessageRedisProperties.java`:

```java
package com.joypeb.chatservice.chatmessage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat.messages.redis")
public record ChatMessageRedisProperties(
	String streamKey
) {
	public ChatMessageRedisProperties {
		if (streamKey == null || streamKey.isBlank()) {
			streamKey = "stream:chat:message-created";
		}
	}
}
```

- [ ] **Step 5: Add Redis publisher implementation**

Create `RedisChatMessagePublisher.java`:

```java
package com.joypeb.chatservice.chatmessage.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joypeb.chatservice.chatmessage.application.ChatMessagePublisher;
import com.joypeb.chatservice.chatmessage.config.ChatMessageRedisProperties;
import com.joypeb.chatservice.chatmessage.event.ChatMessageCreatedEvent;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(ChatMessageRedisProperties.class)
public class RedisChatMessagePublisher implements ChatMessagePublisher {

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final ChatMessageRedisProperties properties;

	public RedisChatMessagePublisher(
		StringRedisTemplate redisTemplate,
		ObjectMapper objectMapper,
		ChatMessageRedisProperties properties
	) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		this.properties = properties;
	}

	@Override
	public void publishCreated(ChatMessageCreatedEvent event) {
		try {
			String payload = objectMapper.writeValueAsString(event.payload());
			MapRecord<String, String, String> record = MapRecord.create(
				properties.streamKey(),
				Map.of(
					"eventId", event.eventId(),
					"eventType", event.eventType(),
					"aggregateId", event.aggregateId().toString(),
					"occurredAt", event.occurredAt().toString(),
					"schemaVersion", Integer.toString(event.schemaVersion()),
					"payload", payload,
					"traceId", event.traceId()
				)
			);
			redisTemplate.opsForStream().add(record);
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize chat message event.", exception);
		}
	}
}
```

- [ ] **Step 6: Publish after DB commit**

Modify `ChatMessageService` constructor to accept `ChatMessagePublisher`. After saving the message, register transaction synchronization:

```java
import com.joypeb.chatservice.chatmessage.event.ChatMessageCreatedEvent;
import java.util.UUID;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
```

Add field:

```java
private final ChatMessagePublisher chatMessagePublisher;
```

Add constructor parameter and assignment:

```java
ChatMessagePublisher chatMessagePublisher,
```

```java
this.chatMessagePublisher = chatMessagePublisher;
```

Replace the end of `send`:

```java
ChatMessage saved = chatMessageRepository.save(message);
ChatMessageCreatedEvent event = ChatMessageCreatedEvent.from(
	UUID.randomUUID().toString(),
	saved.getId(),
	saved.getRoomId(),
	saved.getSenderId(),
	saved.getSequence(),
	saved.getType().name(),
	saved.getContent(),
	saved.getCreatedAt(),
	now,
	UUID.randomUUID().toString()
);
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
	@Override
	public void afterCommit() {
		chatMessagePublisher.publishCreated(event);
	}
});
return toResponse(saved);
```

- [ ] **Step 7: Write publisher invocation test**

Create `ChatMessageServiceTest.java` with a mocked publisher. The test should create a room and member using repositories, call `send`, commit the transaction through the service method, and verify `publishCreated` was called once with `sequence == 1`.

Use `@SpringBootTest`, `@MockBean ChatMessagePublisher`, and repository setup matching existing chat room tests.

- [ ] **Step 8: Run tests**

Run:

```bash
cd chat-service
./gradlew test
```

Expected: all chat-service tests pass without requiring a real Redis server because publisher is mocked where the service test needs verification. If full context creates `RedisChatMessagePublisher`, provide a test profile bean for `ChatMessagePublisher` in controller tests.

- [ ] **Step 9: Commit Redis Stream publisher**

```bash
git add chat-service/build.gradle chat-service/src/main/resources/application.yaml chat-service/src/main/java/com/joypeb/chatservice/chatmessage chat-service/src/test/java/com/joypeb/chatservice/chatmessage
git commit -m "[FEAT] Publish chat message stream events"
```

---

## Task 5: Add Gateway STOMP Send Forwarding

**Files:**
- Create: `gateway/src/main/java/com/joypeb/gateway/chatmessage/dto/stomp/ChatMessageStompSendRequest.java`
- Create: `gateway/src/main/java/com/joypeb/gateway/chatmessage/dto/stomp/ChatMessageStompAckPayload.java`
- Create: `gateway/src/main/java/com/joypeb/gateway/chatmessage/client/ChatMessageClient.java`
- Create: `gateway/src/main/java/com/joypeb/gateway/chatmessage/client/RestChatMessageClient.java`
- Create: `gateway/src/main/java/com/joypeb/gateway/chatmessage/api/stomp/ChatMessageStompController.java`
- Modify: `gateway/build.gradle`
- Test: `gateway/src/test/java/com/joypeb/gateway/chatmessage/api/stomp/ChatMessageStompControllerTest.java`

- [ ] **Step 1: Add HTTP client dependency if needed**

If `RestClient` is unavailable from current webmvc starter, add the web dependency that provides it. Prefer Spring `RestClient` from Spring Web already present before adding new dependencies.

- [ ] **Step 2: Add STOMP request and ACK DTOs**

Create `ChatMessageStompSendRequest.java`:

```java
package com.joypeb.gateway.chatmessage.dto.stomp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ChatMessageStompSendRequest(
	@NotBlank
	@Size(max = 64)
	String requestId,

	@NotBlank
	String type,

	@NotNull
	Payload payload,

	@NotNull
	Instant sentAt
) {
	public record Payload(
		@NotBlank
		@Size(max = 2000)
		String content
	) {
	}
}
```

Create `ChatMessageStompAckPayload.java`:

```java
package com.joypeb.gateway.chatmessage.dto.stomp;

import java.util.UUID;

public record ChatMessageStompAckPayload(
	String requestId,
	UUID messageId,
	UUID roomId,
	long sequence
) {
}
```

- [ ] **Step 3: Add chat message client**

Create `ChatMessageClient.java`:

```java
package com.joypeb.gateway.chatmessage.client;

import java.util.UUID;

public interface ChatMessageClient {

	SentMessage send(String senderId, UUID roomId, String requestId, String type, String content);

	record SentMessage(
		UUID messageId,
		UUID roomId,
		long sequence
	) {
	}
}
```

Create `RestChatMessageClient.java`:

```java
package com.joypeb.gateway.chatmessage.client;

import com.joypeb.gateway.config.ChatServiceProperties;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestChatMessageClient implements ChatMessageClient {

	private static final String ACTOR_HEADER = "X-User-Id";

	private final RestClient restClient;

	public RestChatMessageClient(ChatServiceProperties properties, RestClient.Builder builder) {
		this.restClient = builder.baseUrl(properties.baseUrl()).build();
	}

	@Override
	public SentMessage send(String senderId, UUID roomId, String requestId, String type, String content) {
		ChatServiceMessageResponse response = restClient.post()
			.uri("/internal/chat-rooms/{roomId}/messages", roomId)
			.header(ACTOR_HEADER, senderId)
			.contentType(MediaType.APPLICATION_JSON)
			.body(new ChatServiceMessageRequest(requestId, type, content))
			.retrieve()
			.body(ChatServiceApiResponse.class)
			.data();
		return new SentMessage(response.id(), response.roomId(), response.sequence());
	}

	private record ChatServiceMessageRequest(String requestId, String type, String content) {
	}

	private record ChatServiceApiResponse(ChatServiceMessageResponse data) {
	}

	private record ChatServiceMessageResponse(UUID id, UUID roomId, long sequence) {
	}
}
```

- [ ] **Step 4: Add STOMP controller**

Create `ChatMessageStompController.java`:

```java
package com.joypeb.gateway.chatmessage.api.stomp;

import com.joypeb.gateway.chatmessage.client.ChatMessageClient;
import com.joypeb.gateway.chatmessage.dto.stomp.ChatMessageStompAckPayload;
import com.joypeb.gateway.chatmessage.dto.stomp.ChatMessageStompSendRequest;
import com.joypeb.gateway.dto.stomp.StompServerEvent;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Validated
@Controller
public class ChatMessageStompController {

	private final ChatMessageClient chatMessageClient;
	private final Clock clock;

	public ChatMessageStompController(ChatMessageClient chatMessageClient, Clock clock) {
		this.chatMessageClient = chatMessageClient;
		this.clock = clock;
	}

	@MessageMapping("/chat-rooms/{roomId}/messages")
	@SendToUser("/queue/chat/acks")
	public StompServerEvent<ChatMessageStompAckPayload> send(
		@DestinationVariable UUID roomId,
		@Valid @Payload ChatMessageStompSendRequest request,
		Principal principal
	) {
		ChatMessageClient.SentMessage sent = chatMessageClient.send(
			principal.getName(),
			roomId,
			request.requestId(),
			request.type(),
			request.payload().content()
		);
		return new StompServerEvent<>(
			UUID.randomUUID().toString(),
			"CHAT_MESSAGE_ACCEPTED",
			new ChatMessageStompAckPayload(request.requestId(), sent.messageId(), sent.roomId(), sent.sequence()),
			Instant.now(clock),
			UUID.randomUUID().toString()
		);
	}
}
```

- [ ] **Step 5: Write controller unit test with mocked client**

Create a Spring MVC/STOMP controller test that instantiates `ChatMessageStompController` with a fake `ChatMessageClient`, sends a request object directly, and asserts:

```java
assertThat(event.type()).isEqualTo("CHAT_MESSAGE_ACCEPTED");
assertThat(event.payload().requestId()).isEqualTo("req-1");
assertThat(event.payload().sequence()).isEqualTo(1);
```

Use the existing `StompServerEvent` accessor names from `gateway/src/main/java/com/joypeb/gateway/dto/stomp/StompServerEvent.java`.

- [ ] **Step 6: Run gateway tests**

Run:

```bash
cd gateway
./gradlew test
```

Expected: all gateway tests pass.

- [ ] **Step 7: Commit STOMP send forwarding**

```bash
git add gateway/src/main/java/com/joypeb/gateway/chatmessage gateway/src/test/java/com/joypeb/gateway/chatmessage gateway/build.gradle
git commit -m "[FEAT] Forward chat messages from STOMP"
```

---

## Task 6: Add Gateway Redis Stream Consumer And STOMP Broadcast

**Files:**
- Modify: `gateway/build.gradle`
- Modify: `gateway/src/main/resources/application.yaml`
- Create: `gateway/src/main/java/com/joypeb/gateway/chatmessage/config/ChatMessageStreamProperties.java`
- Create: `gateway/src/main/java/com/joypeb/gateway/chatmessage/event/ChatMessageCreatedStreamEvent.java`
- Create: `gateway/src/main/java/com/joypeb/gateway/chatmessage/dto/stomp/ChatMessageBroadcastPayload.java`
- Create: `gateway/src/main/java/com/joypeb/gateway/chatmessage/stream/ChatMessageStreamConsumer.java`
- Test: `gateway/src/test/java/com/joypeb/gateway/chatmessage/stream/ChatMessageStreamConsumerTest.java`

- [ ] **Step 1: Add Redis dependency**

Modify `gateway/build.gradle`:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

- [ ] **Step 2: Add stream settings**

Modify `gateway/src/main/resources/application.yaml`:

```yaml
spring:
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:localhost}
      port: ${SPRING_DATA_REDIS_PORT:6379}

gateway:
  chat-messages:
    stream-key: ${CHAT_MESSAGE_STREAM_KEY:stream:chat:message-created}
    instance-id: ${GATEWAY_INSTANCE_ID:${HOSTNAME:local}}
    group-prefix: ${CHAT_MESSAGE_STREAM_GROUP_PREFIX:gateway-broadcast}
    consumer-name: ${CHAT_MESSAGE_STREAM_CONSUMER_NAME:${HOSTNAME:local}}
    batch-size: ${CHAT_MESSAGE_STREAM_BATCH_SIZE:20}
    block-timeout: ${CHAT_MESSAGE_STREAM_BLOCK_TIMEOUT:2s}
```

Merge with existing `gateway` and `spring` sections.

- [ ] **Step 3: Add properties**

Create `ChatMessageStreamProperties.java`:

```java
package com.joypeb.gateway.chatmessage.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.chat-messages")
public record ChatMessageStreamProperties(
	String streamKey,
	String instanceId,
	String groupPrefix,
	String consumerName,
	int batchSize,
	Duration blockTimeout
) {
	public ChatMessageStreamProperties {
		if (streamKey == null || streamKey.isBlank()) {
			streamKey = "stream:chat:message-created";
		}
		if (instanceId == null || instanceId.isBlank()) {
			instanceId = "local";
		}
		if (groupPrefix == null || groupPrefix.isBlank()) {
			groupPrefix = "gateway-broadcast";
		}
		if (consumerName == null || consumerName.isBlank()) {
			consumerName = instanceId;
		}
		if (batchSize < 1) {
			batchSize = 20;
		}
		if (blockTimeout == null || blockTimeout.isNegative() || blockTimeout.isZero()) {
			blockTimeout = Duration.ofSeconds(2);
		}
	}

	public String groupName() {
		return groupPrefix + "-" + instanceId;
	}
}
```

- [ ] **Step 4: Add broadcast payload and event DTO**

Create `ChatMessageBroadcastPayload.java`:

```java
package com.joypeb.gateway.chatmessage.dto.stomp;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageBroadcastPayload(
	UUID messageId,
	UUID roomId,
	String senderId,
	long sequence,
	String type,
	String content,
	Instant createdAt
) {
}
```

Create `ChatMessageCreatedStreamEvent.java`:

```java
package com.joypeb.gateway.chatmessage.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatMessageCreatedStreamEvent(
	String eventId,
	String eventType,
	UUID aggregateId,
	Instant occurredAt,
	int schemaVersion,
	Payload payload,
	String traceId
) {
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Payload(
		UUID messageId,
		UUID roomId,
		String senderId,
		long sequence,
		String type,
		String content,
		Instant createdAt
	) {
	}
}
```

- [ ] **Step 5: Add stream consumer**

Create `ChatMessageStreamConsumer.java`:

```java
package com.joypeb.gateway.chatmessage.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joypeb.gateway.chatmessage.config.ChatMessageStreamProperties;
import com.joypeb.gateway.chatmessage.dto.stomp.ChatMessageBroadcastPayload;
import com.joypeb.gateway.chatmessage.event.ChatMessageCreatedStreamEvent;
import com.joypeb.gateway.dto.stomp.StompServerEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(ChatMessageStreamProperties.class)
public class ChatMessageStreamConsumer implements SmartLifecycle {

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final SimpMessagingTemplate messagingTemplate;
	private final ChatMessageStreamProperties properties;
	private final Clock clock;
	private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
	private boolean running;

	public ChatMessageStreamConsumer(
		StringRedisTemplate redisTemplate,
		ObjectMapper objectMapper,
		SimpMessagingTemplate messagingTemplate,
		ChatMessageStreamProperties properties,
		Clock clock
	) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		this.messagingTemplate = messagingTemplate;
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	public void start() {
		createGroupIfMissing();
		var options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
			.builder()
			.pollTimeout(properties.blockTimeout())
			.batchSize(properties.batchSize())
			.build();
		container = StreamMessageListenerContainer.create(redisTemplate.getConnectionFactory(), options);
		container.receive(
			Consumer.from(properties.groupName(), properties.consumerName()),
			StreamOffset.create(properties.streamKey(), ReadOffset.lastConsumed()),
			this::handle
		);
		container.start();
		running = true;
	}

	@Override
	public void stop() {
		if (container != null) {
			container.stop();
		}
		running = false;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	void handle(MapRecord<String, String, String> record) {
		try {
			Map<String, String> value = record.getValue();
			ChatMessageCreatedStreamEvent.Payload payload = objectMapper.readValue(
				value.get("payload"),
				ChatMessageCreatedStreamEvent.Payload.class
			);
			StompServerEvent<ChatMessageBroadcastPayload> event = new StompServerEvent<>(
				value.get("eventId"),
				"CHAT_MESSAGE_CREATED",
				new ChatMessageBroadcastPayload(
					payload.messageId(),
					payload.roomId(),
					payload.senderId(),
					payload.sequence(),
					payload.type(),
					payload.content(),
					payload.createdAt()
				),
				Instant.now(clock),
				value.get("traceId")
			);
			messagingTemplate.convertAndSend("/topic/chat-rooms/" + payload.roomId() + "/messages", event);
			redisTemplate.opsForStream().acknowledge(properties.streamKey(), properties.groupName(), record.getId());
		}
		catch (Exception exception) {
			throw new IllegalStateException("Failed to consume chat message stream record " + record.getId(), exception);
		}
	}

	private void createGroupIfMissing() {
		try {
			redisTemplate.opsForStream().createGroup(properties.streamKey(), ReadOffset.latest(), properties.groupName());
		}
		catch (RedisSystemException exception) {
			if (exception.getMessage() == null || !exception.getMessage().contains("BUSYGROUP")) {
				throw exception;
			}
		}
	}
}
```

- [ ] **Step 6: Write consumer mapping test**

Create `ChatMessageStreamConsumerTest.java` with mocked `StringRedisTemplate` and `SimpMessagingTemplate`, then call `handle(record)` directly. Assert:

```java
verify(messagingTemplate).convertAndSend(
	eq("/topic/chat-rooms/" + roomId + "/messages"),
	argThat(event -> ((StompServerEvent<?>) event).type().equals("CHAT_MESSAGE_CREATED"))
);
verify(streamOperations).acknowledge("stream:chat:message-created", "gateway-broadcast-test-gateway", record.getId());
```

Use an instance id of `test-gateway` so group name is `gateway-broadcast-test-gateway`.

- [ ] **Step 7: Run gateway tests**

Run:

```bash
cd gateway
./gradlew test
```

Expected: all gateway tests pass.

- [ ] **Step 8: Commit stream consumer**

```bash
git add gateway/build.gradle gateway/src/main/resources/application.yaml gateway/src/main/java/com/joypeb/gateway/chatmessage gateway/src/test/java/com/joypeb/gateway/chatmessage
git commit -m "[FEAT] Broadcast chat messages from Redis Stream"
```

---

## Task 7: Tighten STOMP Destination Authorization

**Files:**
- Modify: `gateway/src/main/java/com/joypeb/gateway/websocket/security/StompAuthenticationChannelInterceptor.java`
- Test: `gateway/src/test/java/com/joypeb/gateway/websocket/GatewayWebSocketIntegrationTest.java`

- [ ] **Step 1: Add tests for allowed chat destinations**

Extend `GatewayWebSocketIntegrationTest` with cases that verify:

```text
SUBSCRIBE /topic/chat-rooms/{roomId}/messages is allowed
SUBSCRIBE /user/queue/chat/acks is allowed
SUBSCRIBE /user/queue/chat/errors is allowed
SEND /app/chat-rooms/{roomId}/messages is allowed
SEND /topic/chat-rooms/{roomId}/messages is rejected
SUBSCRIBE /queue/internal is rejected
```

- [ ] **Step 2: Update interceptor rules**

Keep `/topic/` and `/user/queue/` subscribe prefixes, but add explicit path validation for chat destinations:

```java
private static final Pattern CHAT_ROOM_MESSAGES_TOPIC = Pattern.compile(
	"^/topic/chat-rooms/[0-9a-fA-F-]{36}/messages$"
);
private static final Pattern CHAT_ROOM_MESSAGES_SEND = Pattern.compile(
	"^/app/chat-rooms/[0-9a-fA-F-]{36}/messages$"
);
```

In `requireAllowedSubscribeDestination`, accept:

```java
destination.equals("/user/queue/chat/acks")
	|| destination.equals("/user/queue/chat/errors")
	|| destination.equals("/user/queue/gateway/acks")
	|| CHAT_ROOM_MESSAGES_TOPIC.matcher(destination).matches()
```

In `requireApplicationDestination`, accept:

```java
destination.equals("/app/gateway/acks")
	|| CHAT_ROOM_MESSAGES_SEND.matcher(destination).matches()
```

- [ ] **Step 3: Run gateway tests**

Run:

```bash
cd gateway
./gradlew test
```

Expected: all gateway tests pass and invalid destinations fail.

- [ ] **Step 4: Commit authorization tightening**

```bash
git add gateway/src/main/java/com/joypeb/gateway/websocket/security/StompAuthenticationChannelInterceptor.java gateway/src/test/java/com/joypeb/gateway/websocket/GatewayWebSocketIntegrationTest.java
git commit -m "[SECURITY] Restrict chat STOMP destinations"
```

---

## Task 8: Route REST Message History Through Gateway

**Files:**
- Modify: `gateway/src/main/java/com/joypeb/gateway/config/ChatServiceGatewayRouteConfig.java`
- Test: `gateway/src/test/java/com/joypeb/gateway/api/rest/ChatRoomGatewayRouteTest.java`

- [ ] **Step 1: Add routing test**

Extend `ChatRoomGatewayRouteTest` so authenticated `GET /api/v1/chat-rooms/{roomId}/messages?afterSequence=1&size=20` forwards to chat-service with:

```text
same path
same query string
X-User-Id from session
no client-supplied X-User-Id
```

- [ ] **Step 2: Update gateway route predicate if needed**

If current route only matches `/api/v1/chat-rooms` and `/api/v1/chat-rooms/**`, no code change is required for history. If the route is narrower, include `/api/v1/chat-rooms/{roomId}/messages` under the existing chat-service route.

- [ ] **Step 3: Run gateway tests**

Run:

```bash
cd gateway
./gradlew test --tests '*ChatRoomGatewayRouteTest'
./gradlew test
```

Expected: route tests and full gateway suite pass.

- [ ] **Step 4: Commit route coverage**

```bash
git add gateway/src/main/java/com/joypeb/gateway/config/ChatServiceGatewayRouteConfig.java gateway/src/test/java/com/joypeb/gateway/api/rest/ChatRoomGatewayRouteTest.java
git commit -m "[TEST] Cover chat message history routing"
```

---

## Task 9: Document Message And Realtime Contracts

**Files:**
- Modify: `docs/architecture/overview.md`
- Create: `docs/services/chat-service/features/chat-messages.md`
- Modify: `docs/services/gateway/features/local-session-and-stomp-gateway.md`

- [ ] **Step 1: Document chat-service feature**

Create `docs/services/chat-service/features/chat-messages.md` with:

```markdown
# Chat Messages

## 기능 목적

채팅 메시지 기능은 방 멤버가 텍스트 메시지를 전송하고, 서버가 방별 sequence를 부여해 PostgreSQL에 저장하며, 저장된 메시지 생성 이벤트를 Redis Stream으로 발행해 gateway의 STOMP broadcast를 가능하게 한다.

## 전체 처리 흐름

1. gateway가 `POST /internal/chat-rooms/{roomId}/messages`로 메시지 저장을 요청한다.
2. chat-service가 방 존재 여부와 활성 멤버십을 검증한다.
3. `chat_message_sequences`에서 방별 sequence를 pessimistic lock으로 발급한다.
4. `chat_messages`에 메시지를 저장한다.
5. DB commit 이후 `stream:chat:message-created`에 `CHAT_MESSAGE_CREATED` 이벤트를 발행한다.
6. 클라이언트는 재접속 후 `GET /api/v1/chat-rooms/{roomId}/messages?afterSequence={sequence}&size={size}`로 누락 메시지를 복구한다.

## 핵심 로직과 주요 분기

- 메시지 원천 데이터는 `chat_messages`다.
- Redis Stream id는 비즈니스 id가 아니며, `eventId`와 `messageId`를 별도로 사용한다.
- 메시지 sequence는 `roomId` 안에서만 증가한다.
- 비멤버는 메시지 전송과 history 조회가 모두 `403 CHAT_ROOM_FORBIDDEN`이다.
- 삭제되었거나 존재하지 않는 방은 `404 CHAT_ROOM_NOT_FOUND`다.

## 관련 코드 파일 경로

- `/Users/parkeunbin/Desktop/project/redis-chat-test/chat-service/src/main/java/com/joypeb/chatservice/chatmessage/api/ChatMessageController.java`
- `/Users/parkeunbin/Desktop/project/redis-chat-test/chat-service/src/main/java/com/joypeb/chatservice/chatmessage/application/ChatMessageService.java`
- `/Users/parkeunbin/Desktop/project/redis-chat-test/chat-service/src/main/java/com/joypeb/chatservice/chatmessage/domain/ChatMessage.java`
- `/Users/parkeunbin/Desktop/project/redis-chat-test/chat-service/src/main/java/com/joypeb/chatservice/chatmessage/domain/ChatMessageSequence.java`
- `/Users/parkeunbin/Desktop/project/redis-chat-test/chat-service/src/main/java/com/joypeb/chatservice/chatmessage/infrastructure/RedisChatMessagePublisher.java`
- `/Users/parkeunbin/Desktop/project/redis-chat-test/chat-service/src/main/resources/db/migration/V2__create_chat_messages.sql`

## 외부 계약

### REST

- `POST /internal/chat-rooms/{roomId}/messages`
  - header: `X-User-Id`
  - request: `{ "requestId": "req-1", "type": "TEXT", "content": "hello" }`
  - response: `201 Created`
- `GET /api/v1/chat-rooms/{roomId}/messages?afterSequence=0&size=50`
  - header: `X-User-Id`
  - response: `200 OK`

### Redis Stream

- key: `stream:chat:message-created`
- eventType: `CHAT_MESSAGE_CREATED`
- fields: `eventId`, `eventType`, `aggregateId`, `occurredAt`, `schemaVersion`, `payload`, `traceId`
- payload: `messageId`, `roomId`, `senderId`, `sequence`, `type`, `content`, `createdAt`

### DB

- `chat_message_sequences`
- `chat_messages`

## 실패 처리와 예외 상황

- 비멤버 전송과 조회는 `403 CHAT_ROOM_FORBIDDEN`이다.
- 존재하지 않거나 삭제된 방은 `404 CHAT_ROOM_NOT_FOUND`다.
- blank 또는 2000자를 초과한 메시지는 `400 CHAT_MESSAGE_CONTENT_INVALID`다.
- Redis Stream 발행 실패는 DB 저장 성공 이후 발생할 수 있으므로 운영에서는 outbox 재시도 도입을 검토한다.

## 테스트 및 검증 방법

- `ChatMessageControllerTest`로 메시지 저장, sequence 증가, 비멤버 차단, history 조회를 검증한다.
- `ChatMessageServiceTest`로 DB commit 이후 Redis Stream publisher 호출을 검증한다.

## 중요한 설계 결정과 trade-off

- PostgreSQL을 메시지 원천 저장소로 사용하고 Redis Stream은 realtime delivery event로만 사용한다.
- sequence는 방 단위로만 보장한다.
- gateway 장애 중 missed realtime event는 REST history로 복구한다.
- 이번 구현은 transaction synchronization으로 commit 이후 publish한다. Redis 장애 시 자동 재발행까지 요구되면 outbox worker를 추가한다.
```

- [ ] **Step 2: Update architecture overview**

Add a section to `docs/architecture/overview.md`:

```markdown
## Realtime Chat Message Flow

채팅 메시지는 `chat-service`가 PostgreSQL에 저장하고 방별 sequence를 부여한다. 저장 commit 이후 `stream:chat:message-created` Redis Stream에 `CHAT_MESSAGE_CREATED` 이벤트를 발행한다.

gateway는 instance별 consumer group을 사용한다. 예를 들어 `gateway-1`은 `gateway-broadcast-gateway-1`, `gateway-2`는 `gateway-broadcast-gateway-2` group으로 같은 stream을 읽는다. Redis Streams의 단일 consumer group은 메시지를 consumer 중 하나에게만 분배하므로, 다중 gateway broadcast에는 공유 group을 사용하지 않는다.

각 gateway는 수신한 stream event를 자기 instance에 연결된 WebSocket client에게 `/topic/chat-rooms/{roomId}/messages`로 broadcast한다. gateway가 중단된 동안 놓친 메시지는 STOMP로 복구하지 않고 REST history API로 복구한다.
```

- [ ] **Step 3: Update gateway feature doc**

Add to `docs/services/gateway/features/local-session-and-stomp-gateway.md`:

```markdown
### 채팅 메시지 STOMP 전송과 broadcast

1. 클라이언트는 `/app/chat-rooms/{roomId}/messages`로 메시지를 전송한다.
2. gateway는 authenticated principal을 `X-User-Id`로 변환해 chat-service internal REST endpoint에 전달한다.
3. chat-service가 메시지를 저장하면 gateway는 `/user/queue/chat/acks`로 요청자에게 저장 ACK를 보낸다.
4. chat-service가 Redis Stream에 발행한 `CHAT_MESSAGE_CREATED` 이벤트를 gateway instance별 consumer group이 수신한다.
5. gateway는 `/topic/chat-rooms/{roomId}/messages`로 서버 이벤트를 broadcast한다.

다중 gateway 환경에서는 instance별 consumer group을 사용한다. 하나의 공유 consumer group을 쓰면 한 gateway만 이벤트를 받아 다른 gateway에 연결된 client가 메시지를 받지 못한다.
```

- [ ] **Step 4: Commit documentation**

```bash
git add docs/architecture/overview.md docs/services/chat-service/features/chat-messages.md docs/services/gateway/features/local-session-and-stomp-gateway.md
git commit -m "[DOCS] Document realtime chat messages"
```

---

## Task 10: End-To-End Verification

**Files:**
- No source files expected.

- [ ] **Step 1: Run module tests**

Run:

```bash
cd chat-service
./gradlew test
```

Expected: chat-service tests pass.

Run:

```bash
cd gateway
./gradlew test
```

Expected: gateway tests pass.

- [ ] **Step 2: Build Docker images**

Run:

```bash
docker compose build
```

Expected: gateway and chat-service images build successfully.

- [ ] **Step 3: Run local stack**

Run:

```bash
docker compose up
```

Expected:

```text
postgres healthy
redis healthy
chat-service healthy
gateway healthy
```

- [ ] **Step 4: Verify manual flow**

Use a STOMP client or existing integration test helper:

1. `POST /api/v1/sessions` as `user-1`.
2. `POST /api/v1/chat-rooms` to create a room.
3. `POST /api/v1/chat-rooms/{roomId}/members` to invite `user-2`.
4. Connect two WebSocket clients to `/ws`, one as `user-1`, one as `user-2`.
5. Subscribe both to `/topic/chat-rooms/{roomId}/messages`.
6. Send from user-1 to `/app/chat-rooms/{roomId}/messages`.
7. Verify both clients receive `CHAT_MESSAGE_CREATED` with `sequence: 1`.
8. Call `GET /api/v1/chat-rooms/{roomId}/messages?afterSequence=0&size=20`.
9. Verify the REST history contains the same message with `sequence: 1`.

- [ ] **Step 5: Capture final status**

Run:

```bash
git status --short
```

Expected: no uncommitted changes if every task committed its changes.

---

## Residual Risk And Follow-Up

- Redis Stream publish after DB commit can fail after the message is already stored. This plan keeps the first implementation simpler with transaction synchronization. If durable event publication is required, add a `chat_message_outbox` table and a retrying outbox publisher.
- Per-instance consumer groups can leave old groups in Redis after gateway instance id changes. For production, add group cleanup or stable instance ids.
- Stream replay on gateway restart starts from the group offset. Because WebSocket connections are lost on restart, clients must use REST history for missed messages.
- Message edit/delete, typing indicators, read receipts, and presence are intentionally outside this plan.

## Self-Review

- Spec coverage: The plan covers room-scoped sequence assignment, DB storage, Redis Stream publication, multi-gateway fan-out via instance-specific consumer groups, STOMP broadcast, REST history recovery, and documentation.
- Placeholder scan: The plan avoids undefined future work in implementation steps. Explicit follow-up risks are isolated in the residual risk section.
- Type consistency: `ChatMessageResponse`, `ChatMessageCreatedEvent`, gateway broadcast payload, and STOMP destinations use the same `messageId`, `roomId`, `senderId`, `sequence`, `type`, `content`, and `createdAt` fields.
