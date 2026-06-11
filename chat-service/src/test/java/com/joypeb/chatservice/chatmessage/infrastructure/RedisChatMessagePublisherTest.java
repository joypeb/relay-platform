package com.joypeb.chatservice.chatmessage.infrastructure;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joypeb.chatservice.chatmessage.config.ChatMessageRedisProperties;
import com.joypeb.chatservice.chatmessage.event.ChatMessageCreatedEvent;
import com.joypeb.chatservice.common.config.JacksonConfig;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisChatMessagePublisherTest {

	@Test
	@SuppressWarnings("unchecked")
	void publishCreatedSerializesInstantPayloadAsRedisStreamJson() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		StreamOperations<String, Object, Object> streamOperations = mock(StreamOperations.class);
		when(redisTemplate.opsForStream()).thenReturn(streamOperations);
		ObjectMapper objectMapper = new JacksonConfig().objectMapper();
		var properties = new ChatMessageRedisProperties("stream:chat:message-created");
		var publisher = new RedisChatMessagePublisher(redisTemplate, objectMapper, properties);
		UUID roomId = UUID.randomUUID();
		UUID messageId = UUID.randomUUID();
		Instant createdAt = Instant.parse("2026-06-11T05:51:22Z");
		ChatMessageCreatedEvent event = ChatMessageCreatedEvent.from(
			"event-1",
			messageId,
			roomId,
			"user-1",
			1,
			"TEXT",
			"hello",
			createdAt,
			createdAt,
			"trace-1"
		);

		assertThatCode(() -> publisher.publishCreated(event)).doesNotThrowAnyException();

		verify(streamOperations).add(argThat(record -> {
			MapRecord<String, String, String> mapRecord = (MapRecord<String, String, String>) record;
			return mapRecord.getStream().equals("stream:chat:message-created")
				&& mapRecord.getValue().get("payload").contains("\"createdAt\":\"2026-06-11T05:51:22Z\"");
		}));
	}
}
