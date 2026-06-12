package com.joypeb.chatservice.chatread.infrastructure;

import com.joypeb.chatservice.chatread.config.ChatReadStateRedisProperties;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisChatReadStateStore implements ChatReadStateStore {

	private static final DefaultRedisScript<Long> MAX_STRING_SCRIPT = new DefaultRedisScript<>("""
		local current = redis.call('GET', KEYS[1])
		local incoming = tonumber(ARGV[1])
		if current == false or tonumber(current) < incoming then
		  redis.call('SET', KEYS[1], ARGV[1])
		  return 1
		end
		return 0
		""", Long.class);

	private static final DefaultRedisScript<Long> MAX_HASH_AND_DIRTY_SCRIPT = new DefaultRedisScript<>("""
		local current = redis.call('HGET', KEYS[1], ARGV[1])
		local incoming = tonumber(ARGV[2])
		if current == false or tonumber(current) < incoming then
		  redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
		  redis.call('SADD', KEYS[2], ARGV[3])
		  return 1
		end
		return 0
		""", Long.class);

	private static final DefaultRedisScript<Long> REMOVE_DIRTY_IF_UNCHANGED_SCRIPT = new DefaultRedisScript<>("""
		local current = redis.call('HGET', KEYS[1], ARGV[1])
		if current ~= false and tonumber(current) == tonumber(ARGV[2]) then
		  return redis.call('SREM', KEYS[2], ARGV[3])
		end
		return 0
		""", Long.class);

	private final StringRedisTemplate redisTemplate;
	private final ChatReadStateRedisProperties properties;

	public RedisChatReadStateStore(StringRedisTemplate redisTemplate, ChatReadStateRedisProperties properties) {
		this.redisTemplate = redisTemplate;
		this.properties = properties;
	}

	@Override
	public Optional<Long> findRoomLastSequence(UUID roomId) {
		return parse(redisTemplate.opsForValue().get(roomKey(roomId)));
	}

	@Override
	public void warmRoomLastSequence(UUID roomId, long sequence) {
		redisTemplate.opsForValue().set(roomKey(roomId), Long.toString(sequence));
	}

	@Override
	public boolean updateRoomLastSequence(UUID roomId, long sequence) {
		Long result = redisTemplate.execute(MAX_STRING_SCRIPT, List.of(roomKey(roomId)), Long.toString(sequence));
		return result != null && result == 1L;
	}

	@Override
	public Optional<Long> findUserReadSequence(String userId, UUID roomId) {
		return parse((String) redisTemplate.opsForHash().get(userKey(userId), roomId.toString()));
	}

	@Override
	public void warmUserReadSequence(String userId, UUID roomId, long sequence) {
		redisTemplate.opsForHash().put(userKey(userId), roomId.toString(), Long.toString(sequence));
	}

	@Override
	public boolean advanceUserReadSequence(String userId, UUID roomId, long sequence) {
		String member = dirtyMember(roomId, userId);
		Long result = redisTemplate.execute(
			MAX_HASH_AND_DIRTY_SCRIPT,
			List.of(userKey(userId), properties.dirtySetKey()),
			roomId.toString(),
			Long.toString(sequence),
			member
		);
		return result != null && result == 1L;
	}

	@Override
	public Set<String> dirtyMembers(int limit) {
		Set<String> members = redisTemplate.opsForSet().distinctRandomMembers(properties.dirtySetKey(), limit);
		return members == null ? Set.of() : members;
	}

	@Override
	public boolean removeDirtyIfSequenceUnchanged(String member, long flushedSequence) {
		DirtyReadStateMember parsed = DirtyReadStateMember.parse(member);
		Long result = redisTemplate.execute(
			REMOVE_DIRTY_IF_UNCHANGED_SCRIPT,
			List.of(userKey(parsed.userId()), properties.dirtySetKey()),
			parsed.roomId().toString(),
			Long.toString(flushedSequence),
			member
		);
		return result != null && result > 0;
	}

	private String roomKey(UUID roomId) {
		return properties.roomLastSequenceKeyPattern().formatted(roomId);
	}

	private String userKey(String userId) {
		return properties.userRoomReadSequencesKeyPattern().formatted(userId);
	}

	public static String dirtyMember(UUID roomId, String userId) {
		return roomId + ":" + userId;
	}

	private Optional<Long> parse(String value) {
		if (value == null || value.isBlank()) {
			return Optional.empty();
		}
		return Optional.of(Long.parseLong(value));
	}
}
