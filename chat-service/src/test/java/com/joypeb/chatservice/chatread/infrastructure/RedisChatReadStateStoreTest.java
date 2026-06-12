package com.joypeb.chatservice.chatread.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joypeb.chatservice.chatread.config.ChatReadStateRedisProperties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisChatReadStateStoreTest {

	@Test
	@SuppressWarnings("unchecked")
	void advanceUserReadSequenceReturnsTrueOnlyWhenRedisScriptIncreasesValue() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		when(redisTemplate.execute(any(), anyList(), anyString(), anyString(), anyString())).thenReturn(1L, 0L);
		RedisChatReadStateStore store = new RedisChatReadStateStore(redisTemplate, new ChatReadStateRedisProperties());
		UUID roomId = UUID.randomUUID();

		boolean increased = store.advanceUserReadSequence("user-1", roomId, 10);
		boolean stale = store.advanceUserReadSequence("user-1", roomId, 8);

		assertThat(increased).isTrue();
		assertThat(stale).isFalse();
	}

	@Test
	@SuppressWarnings("unchecked")
	void updateRoomLastSequenceIsMonotonic() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(1L, 0L);
		RedisChatReadStateStore store = new RedisChatReadStateStore(redisTemplate, new ChatReadStateRedisProperties());
		UUID roomId = UUID.randomUUID();

		assertThat(store.updateRoomLastSequence(roomId, 5)).isTrue();
		assertThat(store.updateRoomLastSequence(roomId, 4)).isFalse();
	}

	private static java.util.List<String> anyList() {
		return org.mockito.ArgumentMatchers.anyList();
	}
}
