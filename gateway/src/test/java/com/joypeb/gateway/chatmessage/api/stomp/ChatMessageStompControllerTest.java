package com.joypeb.gateway.chatmessage.api.stomp;

import static org.assertj.core.api.Assertions.assertThat;

import com.joypeb.gateway.chatmessage.client.ChatMessageClient;
import com.joypeb.gateway.chatmessage.dto.stomp.ChatMessageStompSendRequest;
import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatMessageStompControllerTest {

	@Test
	void sendForwardsMessageAndReturnsAckEvent() {
		UUID roomId = UUID.randomUUID();
		UUID messageId = UUID.randomUUID();
		ChatMessageClient client = (senderId, requestedRoomId, requestId, type, content) -> {
			assertThat(senderId).isEqualTo("user-1");
			assertThat(requestedRoomId).isEqualTo(roomId);
			assertThat(requestId).isEqualTo("req-1");
			assertThat(type).isEqualTo("TEXT");
			assertThat(content).isEqualTo("hello");
			return new ChatMessageClient.SentMessage(messageId, roomId, 1);
		};
		var controller = new ChatMessageStompController(
			client,
			Clock.fixed(Instant.parse("2026-06-11T00:00:00Z"), ZoneOffset.UTC)
		);

		var event = controller.send(
			roomId,
			new ChatMessageStompSendRequest(
				"req-1",
				"TEXT",
				new ChatMessageStompSendRequest.Payload("hello"),
				Instant.parse("2026-06-11T00:00:00Z")
			),
			(Principal) () -> "user-1"
		);

		assertThat(event.type()).isEqualTo("CHAT_MESSAGE_ACCEPTED");
		assertThat(event.payload().requestId()).isEqualTo("req-1");
		assertThat(event.payload().messageId()).isEqualTo(messageId);
		assertThat(event.payload().sequence()).isEqualTo(1);
	}
}
