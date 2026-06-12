package com.joypeb.chatservice.chatmessage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import com.joypeb.chatservice.chatmessage.dto.ChatMessageSendRequest;
import com.joypeb.chatservice.chatread.application.ChatReadStateService;
import com.joypeb.chatservice.chatroom.domain.ChatRoom;
import com.joypeb.chatservice.chatroom.domain.ChatRoomMember;
import com.joypeb.chatservice.chatroom.domain.ChatRoomVisibility;
import com.joypeb.chatservice.chatroom.infrastructure.ChatRoomMemberRepository;
import com.joypeb.chatservice.chatroom.infrastructure.ChatRoomRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ChatMessageServiceTest {

	@Autowired
	ChatMessageService chatMessageService;

	@Autowired
	ChatRoomRepository chatRoomRepository;

	@Autowired
	ChatRoomMemberRepository chatRoomMemberRepository;

	@MockitoBean
	ChatMessagePublisher chatMessagePublisher;

	@MockitoBean
	ChatReadStateService chatReadStateService;

	@Test
	void sendPublishesCreatedEventAfterCommit() {
		Instant now = Instant.parse("2026-06-11T00:00:00Z");
		ChatRoom room = chatRoomRepository.save(ChatRoom.create("room", "description", "user-1", ChatRoomVisibility.PRIVATE, now));
		chatRoomMemberRepository.save(ChatRoomMember.owner(room.getId(), "user-1", now));

		var response = chatMessageService.send("user-1", room.getId(), new ChatMessageSendRequest("req-1", "TEXT", "hello"));

		assertThat(response.sequence()).isEqualTo(1);
		verify(chatMessagePublisher).publishCreated(argThat(event ->
			event.payload().sequence() == 1
				&& event.payload().messageId().equals(response.id())
				&& event.payload().roomId().equals(room.getId())
		));
		verify(chatReadStateService).recordMessageCommitted("user-1", room.getId(), 1);
	}
}
