package com.joypeb.chatservice.chatread.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatRoomReadStateTest {

	@Test
	void advanceToKeepsLastReadSequenceMonotonic() {
		UUID roomId = UUID.randomUUID();
		Instant now = Instant.parse("2026-06-12T00:00:00Z");
		ChatRoomReadState state = ChatRoomReadState.create(roomId, "user-1", 10, now);

		boolean staleChanged = state.advanceTo(7, now.plusSeconds(1));
		boolean newerChanged = state.advanceTo(12, now.plusSeconds(2));

		assertThat(staleChanged).isFalse();
		assertThat(newerChanged).isTrue();
		assertThat(state.getLastReadSequence()).isEqualTo(12);
		assertThat(state.getUpdatedAt()).isEqualTo(now.plusSeconds(2));
	}
}
