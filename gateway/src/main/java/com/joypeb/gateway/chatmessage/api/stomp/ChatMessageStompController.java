package com.joypeb.gateway.chatmessage.api.stomp;

import com.joypeb.gateway.chatmessage.client.ChatMessageClient;
import com.joypeb.gateway.chatmessage.dto.stomp.ChatReadReceiptStompAckPayload;
import com.joypeb.gateway.chatmessage.dto.stomp.ChatReadReceiptStompRequest;
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

	@MessageMapping("/chat-rooms/{roomId}/read-receipts")
	@SendToUser("/queue/chat/acks")
	public StompServerEvent<ChatReadReceiptStompAckPayload> readReceipt(
		@DestinationVariable UUID roomId,
		@Valid @Payload ChatReadReceiptStompRequest request,
		Principal principal
	) {
		ChatMessageClient.AcceptedReadReceipt receipt = chatMessageClient.readReceipt(
			principal.getName(),
			roomId,
			request.requestId(),
			request.type(),
			request.payload().lastReadSequence()
		);
		return new StompServerEvent<>(
			UUID.randomUUID().toString(),
			"CHAT_MESSAGES_READ_ACCEPTED",
			new ChatReadReceiptStompAckPayload(request.requestId(), receipt.roomId(), receipt.lastReadSequence()),
			Instant.now(clock),
			UUID.randomUUID().toString()
		);
	}
}
