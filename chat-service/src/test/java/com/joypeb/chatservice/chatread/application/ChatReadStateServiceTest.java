package com.joypeb.chatservice.chatread.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.joypeb.chatservice.chatread.infrastructure.ChatRoomReadStateRepository;
import com.joypeb.chatservice.chatread.infrastructure.ChatReadStateStore;
import com.joypeb.chatservice.chatroom.domain.ChatRoom;
import com.joypeb.chatservice.chatroom.domain.ChatRoomMember;
import com.joypeb.chatservice.chatroom.domain.ChatRoomVisibility;
import com.joypeb.chatservice.chatroom.infrastructure.ChatRoomMemberRepository;
import com.joypeb.chatservice.chatroom.infrastructure.ChatRoomRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ChatReadStateServiceTest {

	@Autowired
	ChatReadStateService chatReadStateService;

	@Autowired
	ChatRoomRepository chatRoomRepository;

	@Autowired
	ChatRoomMemberRepository chatRoomMemberRepository;

	@Autowired
	ChatRoomReadStateRepository readStateRepository;

	@MockitoBean
	ChatReadStateStore readStateStore;

	@Test
	void acceptReadReceiptRejectsSequenceGreaterThanRoomLastSequence() {
		ChatRoom room = createRoomWithMember("user-1");
		when(readStateStore.findRoomLastSequence(room.getId())).thenReturn(Optional.of(3L));

		assertThatThrownBy(() -> chatReadStateService.acceptReadReceipt("user-1", room.getId(), 4))
			.isInstanceOf(ChatReadSequenceOutOfRangeException.class);
	}

	@Test
	void acceptReadReceiptReturnsAcceptedSequenceAfterMonotonicRedisUpdate() {
		ChatRoom room = createRoomWithMember("user-1");
		when(readStateStore.findRoomLastSequence(room.getId())).thenReturn(Optional.of(3L));

		var response = chatReadStateService.acceptReadReceipt("user-1", room.getId(), 3);

		assertThat(response.roomId()).isEqualTo(room.getId());
		assertThat(response.lastReadSequence()).isEqualTo(3);
	}

	private ChatRoom createRoomWithMember(String memberId) {
		Instant now = Instant.parse("2026-06-12T00:00:00Z");
		ChatRoom room = chatRoomRepository.save(ChatRoom.create("room", "description", memberId, ChatRoomVisibility.PRIVATE, now));
		chatRoomMemberRepository.save(ChatRoomMember.owner(room.getId(), memberId, now));
		return room;
	}
}
