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
		ChatServiceApiResponse response = restClient.post()
			.uri("/internal/chat-rooms/{roomId}/messages", roomId)
			.header(ACTOR_HEADER, senderId)
			.contentType(MediaType.APPLICATION_JSON)
			.body(new ChatServiceMessageRequest(requestId, type, content))
			.retrieve()
			.body(ChatServiceApiResponse.class);
		if (response == null || response.data() == null) {
			throw new IllegalStateException("Chat service returned an empty message response.");
		}
		ChatServiceMessageResponse message = response.data();
		return new SentMessage(message.id(), message.roomId(), message.sequence());
	}

	private record ChatServiceMessageRequest(String requestId, String type, String content) {
	}

	private record ChatServiceApiResponse(ChatServiceMessageResponse data) {
	}

	private record ChatServiceMessageResponse(UUID id, UUID roomId, long sequence) {
	}
}
