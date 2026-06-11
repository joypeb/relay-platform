package com.joypeb.chatservice.chatroom.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_room_members")
public class ChatRoomMember {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "room_id", nullable = false)
	private UUID roomId;

	@Column(name = "member_id", nullable = false, length = 64)
	private String memberId;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private ChatRoomMemberRole role;

	@Column(name = "joined_at", nullable = false)
	private Instant joinedAt;

	@Column(name = "left_at")
	private Instant leftAt;

	protected ChatRoomMember() {
	}

	private ChatRoomMember(UUID roomId, String memberId, ChatRoomMemberRole role, Instant joinedAt) {
		this.id = UUID.randomUUID();
		this.roomId = roomId;
		this.memberId = memberId;
		this.role = role;
		this.joinedAt = joinedAt;
	}

	public static ChatRoomMember owner(UUID roomId, String memberId, Instant joinedAt) {
		return new ChatRoomMember(roomId, memberId, ChatRoomMemberRole.OWNER, joinedAt);
	}

	public static ChatRoomMember member(UUID roomId, String memberId, Instant joinedAt) {
		return new ChatRoomMember(roomId, memberId, ChatRoomMemberRole.MEMBER, joinedAt);
	}

	@PrePersist
	void prePersist() {
		if (id == null) {
			id = UUID.randomUUID();
		}
	}

	public UUID getId() {
		return id;
	}

	public UUID getRoomId() {
		return roomId;
	}

	public String getMemberId() {
		return memberId;
	}

	public ChatRoomMemberRole getRole() {
		return role;
	}

	public Instant getJoinedAt() {
		return joinedAt;
	}
}
