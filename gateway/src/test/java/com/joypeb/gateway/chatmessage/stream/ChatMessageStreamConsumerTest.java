package com.joypeb.gateway.chatmessage.stream;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.joypeb.gateway.chatmessage.config.ChatMessageStreamProperties;
import com.joypeb.gateway.chatmessage.event.ChatMessageCreatedStreamEvent;
import com.joypeb.gateway.dto.stomp.StompServerEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class ChatMessageStreamConsumerTest {

	@Test
	@SuppressWarnings("unchecked")
	void handleBroadcastsStreamEventAndAcknowledgesRecord() throws Exception {
		UUID roomId = UUID.randomUUID();
		UUID messageId = UUID.randomUUID();
		ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
		var payload = new ChatMessageCreatedStreamEvent.Payload(
			messageId,
			roomId,
			"user-1",
			7,
			"TEXT",
			"hello",
			Instant.parse("2026-06-11T00:00:00Z")
		);
		MapRecord<String, String, String> record = MapRecord.create(
			"stream:chat:message-created",
			Map.of(
				"eventId", "event-1",
				"eventType", "CHAT_MESSAGE_CREATED",
				"aggregateId", roomId.toString(),
				"occurredAt", "2026-06-11T00:00:00Z",
				"schemaVersion", "1",
				"payload", objectMapper.writeValueAsString(payload),
				"traceId", "trace-1"
			)
		).withId(RecordId.of("1-0"));
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		StreamOperations<String, Object, Object> streamOperations = mock(StreamOperations.class);
		when(redisTemplate.opsForStream()).thenReturn(streamOperations);
		SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
		var properties = new ChatMessageStreamProperties(
			"stream:chat:message-created",
			"test-gateway",
			"gateway-broadcast",
			"test-consumer",
			20,
			Duration.ofSeconds(2),
			true
		);
		var consumer = new ChatMessageStreamConsumer(
			redisTemplate,
			objectMapper,
			messagingTemplate,
			properties,
			Clock.fixed(Instant.parse("2026-06-11T00:00:01Z"), ZoneOffset.UTC)
		);

		consumer.handle(record);

		verify(messagingTemplate).convertAndSend(
			eq("/topic/chat-rooms/" + roomId + "/messages"),
			org.mockito.ArgumentMatchers.<Object>argThat(event ->
				((StompServerEvent<?>) event).type().equals("CHAT_MESSAGE_CREATED")
			)
		);
		verify(streamOperations).acknowledge("stream:chat:message-created", "gateway-broadcast-test-gateway", record.getId());
	}

	@Test
	void createGroupCreatesMissingStreamBeforeListening() throws Exception {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		StreamOperations<String, Object, Object> streamOperations = mock(StreamOperations.class);
		when(redisTemplate.opsForStream()).thenReturn(streamOperations);
		RedisConnection connection = mock(RedisConnection.class);
		RedisStreamCommands streamCommands = mock(RedisStreamCommands.class);
		when(connection.streamCommands()).thenReturn(streamCommands);
		when(redisTemplate.execute(org.mockito.ArgumentMatchers.<RedisCallback<String>>any()))
			.thenAnswer(invocation -> invocation.<RedisCallback<String>>getArgument(0).doInRedis(connection));
		var properties = new ChatMessageStreamProperties(
			"stream:chat:message-created",
			"test-gateway",
			"gateway-broadcast",
			"test-consumer",
			20,
			Duration.ofSeconds(2),
			true
		);
		var consumer = new ChatMessageStreamConsumer(
			redisTemplate,
			JsonMapper.builder().findAndAddModules().build(),
			mock(SimpMessagingTemplate.class),
			properties,
			Clock.fixed(Instant.parse("2026-06-11T00:00:01Z"), ZoneOffset.UTC)
		);

		var method = ChatMessageStreamConsumer.class.getDeclaredMethod("createGroupIfMissing");
		method.setAccessible(true);
		method.invoke(consumer);

		verify(streamCommands).xGroupCreate(
			"stream:chat:message-created".getBytes(java.nio.charset.StandardCharsets.UTF_8),
			"gateway-broadcast-test-gateway",
			ReadOffset.latest(),
			true
		);
	}
}
