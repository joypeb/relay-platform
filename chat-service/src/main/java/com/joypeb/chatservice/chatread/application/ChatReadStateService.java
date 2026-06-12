package com.joypeb.chatservice.chatread.application;

import com.joypeb.chatservice.chatmessage.infrastructure.ChatMessageSequenceRepository;
import com.joypeb.chatservice.chatread.domain.ChatRoomReadState;
import com.joypeb.chatservice.chatread.dto.ChatReadReceiptResponse;
import com.joypeb.chatservice.chatread.infrastructure.ChatReadStateStore;
import com.joypeb.chatservice.chatread.infrastructure.ChatRoomReadStateRepository;
import com.joypeb.chatservice.chatroom.application.ChatRoomForbiddenException;
import com.joypeb.chatservice.chatroom.application.ChatRoomNotFoundException;
import com.joypeb.chatservice.chatroom.infrastructure.ChatRoomMemberRepository;
import com.joypeb.chatservice.chatroom.infrastructure.ChatRoomRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatReadStateService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ChatMessageSequenceRepository chatMessageSequenceRepository;
	private final ChatRoomReadStateRepository readStateRepository;
	private final ChatReadStateStore readStateStore;
	private final Clock clock;

	public ChatReadStateService(
		ChatRoomRepository chatRoomRepository,
		ChatRoomMemberRepository chatRoomMemberRepository,
		ChatMessageSequenceRepository chatMessageSequenceRepository,
		ChatRoomReadStateRepository readStateRepository,
		ChatReadStateStore readStateStore,
		Clock clock
	) {
		this.chatRoomRepository = chatRoomRepository;
		this.chatRoomMemberRepository = chatRoomMemberRepository;
		this.chatMessageSequenceRepository = chatMessageSequenceRepository;
		this.readStateRepository = readStateRepository;
		this.readStateStore = readStateStore;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public ChatReadReceiptResponse acceptReadReceipt(String actorId, UUID roomId, String requestId, long lastReadSequence) {
		requireActiveRoom(roomId);
		requireMember(roomId, actorId);
		long lastMessageSequence = resolveRoomLastSequence(roomId);
		if (lastReadSequence > lastMessageSequence) {
			throw new ChatReadSequenceOutOfRangeException(lastReadSequence, lastMessageSequence);
		}
		readStateStore.advanceUserReadSequence(actorId, roomId, lastReadSequence);
		return new ChatReadReceiptResponse(requestId, roomId, lastReadSequence);
	}

	@Transactional(readOnly = true)
	public ChatReadReceiptResponse acceptReadReceipt(String actorId, UUID roomId, long lastReadSequence) {
		return acceptReadReceipt(actorId, roomId, null, lastReadSequence);
	}

	@Transactional(readOnly = true)
	public long resolveRoomLastSequence(UUID roomId) {
		return readStateStore.findRoomLastSequence(roomId)
			.orElseGet(() -> {
				long recovered = chatMessageSequenceRepository.findById(roomId)
					.map(sequence -> Math.max(0, sequence.getNextSequence() - 1))
					.orElse(0L);
				readStateStore.warmRoomLastSequence(roomId, recovered);
				return recovered;
			});
	}

	@Transactional(readOnly = true)
	public long resolveUserReadSequence(String userId, UUID roomId) {
		return readStateStore.findUserReadSequence(userId, roomId)
			.orElseGet(() -> {
				long recovered = readStateRepository.findByRoomIdAndMemberId(roomId, userId)
					.map(ChatRoomReadState::getLastReadSequence)
					.orElse(0L);
				readStateStore.warmUserReadSequence(userId, roomId, recovered);
				return recovered;
			});
	}

	@Transactional(readOnly = true)
	public Map<UUID, Long> resolveRoomLastSequences(Collection<UUID> roomIds) {
		return roomIds.stream().collect(Collectors.toMap(Function.identity(), this::resolveRoomLastSequence));
	}

	@Transactional(readOnly = true)
	public Map<UUID, Long> resolveUserReadSequences(String userId, Collection<UUID> roomIds) {
		return roomIds.stream().collect(Collectors.toMap(Function.identity(), roomId -> resolveUserReadSequence(userId, roomId)));
	}

	public void recordMessageCommitted(String senderId, UUID roomId, long sequence) {
		readStateStore.updateRoomLastSequence(roomId, sequence);
		readStateStore.advanceUserReadSequence(senderId, roomId, sequence);
	}

	@Transactional
	public void saveMonotonic(String memberId, UUID roomId, long lastReadSequence) {
		Instant now = clock.instant();
		ChatRoomReadState state = readStateRepository.findByRoomIdAndMemberId(roomId, memberId)
			.orElseGet(() -> ChatRoomReadState.create(roomId, memberId, 0, now));
		state.advanceTo(lastReadSequence, now);
		readStateRepository.save(state);
	}

	private void requireActiveRoom(UUID roomId) {
		if (chatRoomRepository.findByIdAndDeletedAtIsNull(roomId).isEmpty()) {
			throw new ChatRoomNotFoundException(roomId);
		}
	}

	private void requireMember(UUID roomId, String actorId) {
		if (!chatRoomMemberRepository.existsByRoomIdAndMemberIdAndLeftAtIsNull(roomId, actorId)) {
			throw new ChatRoomForbiddenException();
		}
	}
}
