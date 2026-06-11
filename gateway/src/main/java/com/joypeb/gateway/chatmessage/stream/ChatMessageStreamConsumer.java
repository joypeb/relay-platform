package com.joypeb.gateway.chatmessage.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joypeb.gateway.chatmessage.config.ChatMessageStreamProperties;
import com.joypeb.gateway.chatmessage.dto.stomp.ChatMessageBroadcastPayload;
import com.joypeb.gateway.chatmessage.event.ChatMessageCreatedStreamEvent;
import com.joypeb.gateway.dto.stomp.StompServerEvent;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisCallback;
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
		if (!properties.streamConsumerEnabled()) {
			return;
		}
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
			redisTemplate.execute((RedisCallback<String>) connection -> connection.streamCommands().xGroupCreate(
				properties.streamKey().getBytes(StandardCharsets.UTF_8),
				properties.groupName(),
				ReadOffset.latest(),
				true
			));
		}
		catch (RedisSystemException exception) {
			if (exception.getMessage() == null || !exception.getMessage().contains("BUSYGROUP")) {
				throw exception;
			}
		}
	}
}
