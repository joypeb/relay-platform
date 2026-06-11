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
