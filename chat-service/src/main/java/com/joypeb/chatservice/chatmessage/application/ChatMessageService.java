package com.joypeb.chatservice.chatmessage.application;

import com.joypeb.chatservice.chatmessage.domain.ChatMessage;
import com.joypeb.chatservice.chatmessage.domain.ChatMessageSequence;
import com.joypeb.chatservice.chatmessage.dto.ChatMessageResponse;
import com.joypeb.chatservice.chatmessage.dto.ChatMessageSendRequest;
import com.joypeb.chatservice.chatmessage.event.ChatMessageCreatedEvent;
import com.joypeb.chatservice.chatmessage.infrastructure.ChatMessageRepository;
import com.joypeb.chatservice.chatmessage.infrastructure.ChatMessageSequenceRepository;
import com.joypeb.chatservice.chatroom.application.ChatRoomForbiddenException;
import com.joypeb.chatservice.chatroom.application.ChatRoomNotFoundException;
import com.joypeb.chatservice.chatroom.infrastructure.ChatRoomMemberRepository;
import com.joypeb.chatservice.chatroom.infrastructure.ChatRoomRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ChatMessageService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final ChatMessageSequenceRepository chatMessageSequenceRepository;
	private final ChatMessagePublisher chatMessagePublisher;
	private final Clock clock;

	public ChatMessageService(
		ChatRoomRepository chatRoomRepository,
		ChatRoomMemberRepository chatRoomMemberRepository,
		ChatMessageRepository chatMessageRepository,
		ChatMessageSequenceRepository chatMessageSequenceRepository,
		ChatMessagePublisher chatMessagePublisher,
		Clock clock
	) {
		this.chatRoomRepository = chatRoomRepository;
		this.chatRoomMemberRepository = chatRoomMemberRepository;
		this.chatMessageRepository = chatMessageRepository;
		this.chatMessageSequenceRepository = chatMessageSequenceRepository;
		this.chatMessagePublisher = chatMessagePublisher;
		this.clock = clock;
	}

	@Transactional
	public ChatMessageResponse send(String senderId, UUID roomId, ChatMessageSendRequest request) {
		requireActiveRoom(roomId);
		requireMember(roomId, senderId);
		String content = normalizeContent(request.content());
		ChatMessageSequence sequence = chatMessageSequenceRepository.findWithLockByRoomId(roomId)
			.orElseGet(() -> chatMessageSequenceRepository.saveAndFlush(ChatMessageSequence.create(roomId)));
		Instant now = clock.instant();
		ChatMessage message = ChatMessage.text(roomId, senderId, sequence.issue(), content, now);
		ChatMessage saved = chatMessageRepository.save(message);
		publishAfterCommit(saved, now);
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public List<ChatMessageResponse> history(String actorId, UUID roomId, long afterSequence, int size) {
		requireActiveRoom(roomId);
		requireMember(roomId, actorId);
		int boundedSize = Math.min(Math.max(size, 1), 100);
		return chatMessageRepository
			.findByRoomIdAndSequenceGreaterThanAndDeletedAtIsNullOrderBySequenceAsc(
				roomId,
				afterSequence,
				PageRequest.of(0, boundedSize)
			)
			.stream()
			.map(this::toResponse)
			.toList();
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

	private String normalizeContent(String content) {
		String normalized = content == null ? "" : content.trim();
		if (normalized.isBlank() || normalized.length() > 2000) {
			throw new ChatMessageContentInvalidException();
		}
		return normalized;
	}

	private void publishAfterCommit(ChatMessage saved, Instant occurredAt) {
		ChatMessageCreatedEvent event = ChatMessageCreatedEvent.from(
			UUID.randomUUID().toString(),
			saved.getId(),
			saved.getRoomId(),
			saved.getSenderId(),
			saved.getSequence(),
			saved.getType().name(),
			saved.getContent(),
			saved.getCreatedAt(),
			occurredAt,
			UUID.randomUUID().toString()
		);
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				chatMessagePublisher.publishCreated(event);
			}
		});
	}

	private ChatMessageResponse toResponse(ChatMessage message) {
		return new ChatMessageResponse(
			message.getId(),
			message.getRoomId(),
			message.getSenderId(),
			message.getSequence(),
			message.getType(),
			message.getContent(),
			message.getCreatedAt()
		);
	}
}
