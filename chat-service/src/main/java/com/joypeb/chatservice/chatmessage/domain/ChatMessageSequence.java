package com.joypeb.chatservice.chatmessage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "chat_message_sequences")
public class ChatMessageSequence {

	@Id
	@Column(name = "room_id", nullable = false)
	private UUID roomId;

	@Column(name = "next_sequence", nullable = false)
	private long nextSequence;

	protected ChatMessageSequence() {
	}

	private ChatMessageSequence(UUID roomId) {
		this.roomId = roomId;
		this.nextSequence = 1;
	}

	public static ChatMessageSequence create(UUID roomId) {
		return new ChatMessageSequence(roomId);
	}

	public long issue() {
		long issued = nextSequence;
		nextSequence += 1;
		return issued;
	}

	public UUID getRoomId() {
		return roomId;
	}
}
