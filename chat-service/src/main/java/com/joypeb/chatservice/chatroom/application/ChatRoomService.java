package com.joypeb.chatservice.chatroom.application;

import com.joypeb.chatservice.chatroom.api.ChatRoomListScope;
import com.joypeb.chatservice.chatroom.domain.ChatRoom;
import com.joypeb.chatservice.chatroom.domain.ChatRoomMember;
import com.joypeb.chatservice.chatroom.domain.ChatRoomVisibility;
import com.joypeb.chatservice.chatroom.dto.ChatRoomCreateRequest;
import com.joypeb.chatservice.chatroom.dto.ChatRoomMemberInviteRequest;
import com.joypeb.chatservice.chatroom.dto.ChatRoomMemberResponse;
import com.joypeb.chatservice.chatroom.dto.ChatRoomResponse;
import com.joypeb.chatservice.chatroom.dto.ChatRoomSummaryResponse;
import com.joypeb.chatservice.chatroom.dto.ChatRoomUpdateRequest;
import com.joypeb.chatservice.chatread.application.ChatReadStateService;
import com.joypeb.chatservice.chatroom.infrastructure.ChatRoomMemberRepository;
import com.joypeb.chatservice.chatroom.infrastructure.ChatRoomRepository;
import com.joypeb.chatservice.common.api.PageResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatRoomService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final ChatReadStateService chatReadStateService;
	private final Clock clock;

	public ChatRoomService(
		ChatRoomRepository chatRoomRepository,
		ChatRoomMemberRepository chatRoomMemberRepository,
		ChatReadStateService chatReadStateService,
		Clock clock
	) {
		this.chatRoomRepository = chatRoomRepository;
		this.chatRoomMemberRepository = chatRoomMemberRepository;
		this.chatReadStateService = chatReadStateService;
		this.clock = clock;
	}

	@Transactional
	public ChatRoomResponse create(String actorId, ChatRoomCreateRequest request) {
		Instant now = clock.instant();
		ChatRoom room = ChatRoom.create(
			request.name(),
			request.description(),
			actorId,
			ChatRoomVisibility.fromPubliclyVisible(request.publiclyVisible()),
			now
		);
		ChatRoom savedRoom = chatRoomRepository.save(room);
		chatRoomMemberRepository.save(ChatRoomMember.owner(savedRoom.getId(), actorId, now));
		return toResponse(savedRoom, 1);
	}

	@Transactional(readOnly = true)
	public PageResponse<ChatRoomSummaryResponse> list(String actorId, ChatRoomListScope scope, Pageable pageable) {
		Page<ChatRoom> rooms = switch (scope) {
			case PUBLIC -> chatRoomRepository.findByDeletedAtIsNullAndVisibility(ChatRoomVisibility.PUBLIC, pageable);
			case JOINED -> chatRoomRepository.findJoinedRooms(actorId, pageable);
		};
		Page<ChatRoomSummaryResponse> summaries = rooms.map(room -> {
			long lastMessageSequence = chatReadStateService.resolveRoomLastSequence(room.getId());
			long lastReadSequence = chatReadStateService.resolveUserReadSequence(actorId, room.getId());
			long unreadCount = Math.max(0, lastMessageSequence - lastReadSequence);
			return toSummary(room, memberCount(room.getId()), lastMessageSequence, lastReadSequence, unreadCount);
		});
		return PageResponse.from(summaries);
	}

	@Transactional(readOnly = true)
	public ChatRoomResponse get(String actorId, UUID roomId) {
		ChatRoom room = findActiveRoom(roomId);
		return toResponse(room, memberCount(room.getId()));
	}

	@Transactional
	public ChatRoomResponse update(String actorId, UUID roomId, ChatRoomUpdateRequest request) {
		ChatRoom room = findActiveRoom(roomId);
		room.update(actorId, request.name(), request.description(),
			ChatRoomVisibility.fromPubliclyVisible(request.publiclyVisible()), clock.instant());
		return toResponse(room, memberCount(room.getId()));
	}

	@Transactional
	public ChatRoomMemberResponse inviteMember(String actorId, UUID roomId, ChatRoomMemberInviteRequest request) {
		ChatRoom room = findActiveRoom(roomId);
		if (chatRoomMemberRepository.existsByRoomIdAndMemberIdAndLeftAtIsNull(roomId, request.memberId())) {
			throw new ChatRoomMemberAlreadyExistsException(roomId, request.memberId());
		}
		ChatRoomMember member = room.inviteMember(actorId, request.memberId(), clock.instant());
		try {
			return toMemberResponse(chatRoomMemberRepository.saveAndFlush(member));
		}
		catch (DataIntegrityViolationException exception) {
			throw new ChatRoomMemberAlreadyExistsException(roomId, request.memberId());
		}
	}

	@Transactional
	public void delete(String actorId, UUID roomId) {
		ChatRoom room = findActiveRoom(roomId);
		room.delete(actorId, clock.instant());
	}

	private ChatRoom findActiveRoom(UUID roomId) {
		return chatRoomRepository.findByIdAndDeletedAtIsNull(roomId)
			.orElseThrow(() -> new ChatRoomNotFoundException(roomId));
	}

	private long memberCount(UUID roomId) {
		return chatRoomMemberRepository.countByRoomIdAndLeftAtIsNull(roomId);
	}

	private ChatRoomResponse toResponse(ChatRoom room, long memberCount) {
		return new ChatRoomResponse(
			room.getId(),
			room.getName(),
			room.getDescription(),
			room.getOwnerId(),
			room.getVisibility(),
			room.getStatus(),
			memberCount,
			room.getCreatedAt(),
			room.getUpdatedAt()
		);
	}

	private ChatRoomSummaryResponse toSummary(
		ChatRoom room,
		long memberCount,
		long lastMessageSequence,
		long lastReadSequence,
		long unreadCount
	) {
		return new ChatRoomSummaryResponse(
			room.getId(),
			room.getName(),
			room.getDescription(),
			room.getOwnerId(),
			room.getVisibility(),
			memberCount,
			lastMessageSequence,
			lastReadSequence,
			unreadCount,
			room.getCreatedAt()
		);
	}

	private ChatRoomMemberResponse toMemberResponse(ChatRoomMember member) {
		return new ChatRoomMemberResponse(
			member.getId(),
			member.getRoomId(),
			member.getMemberId(),
			member.getRole(),
			member.getJoinedAt()
		);
	}
}
