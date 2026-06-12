package com.joypeb.chatservice.chatread.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@IdClass(ChatRoomReadStateId.class)
@Table(name = "chat_room_read_states")
public class ChatRoomReadState {

	@Id
	@Column(name = "room_id", nullable = false)
	private UUID roomId;

	@Id
	@Column(name = "member_id", nullable = false, length = 64)
	private String memberId;

	@Column(name = "last_read_sequence", nullable = false)
	private long lastReadSequence;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ChatRoomReadState() {
	}

	private ChatRoomReadState(UUID roomId, String memberId, long lastReadSequence, Instant now) {
		this.roomId = roomId;
		this.memberId = memberId;
		this.lastReadSequence = lastReadSequence;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public static ChatRoomReadState create(UUID roomId, String memberId, long lastReadSequence, Instant now) {
		return new ChatRoomReadState(roomId, memberId, lastReadSequence, now);
	}

	public boolean advanceTo(long sequence, Instant updatedAt) {
		if (sequence <= lastReadSequence) {
			return false;
		}
		this.lastReadSequence = sequence;
		this.updatedAt = updatedAt;
		return true;
	}

	public UUID getRoomId() {
		return roomId;
	}

	public String getMemberId() {
		return memberId;
	}

	public long getLastReadSequence() {
		return lastReadSequence;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
