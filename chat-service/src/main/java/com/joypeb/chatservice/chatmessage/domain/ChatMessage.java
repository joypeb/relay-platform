package com.joypeb.chatservice.chatmessage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "room_id", nullable = false)
	private UUID roomId;

	@Column(name = "sender_id", nullable = false, length = 64)
	private String senderId;

	@Column(name = "sequence", nullable = false)
	private long sequence;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private ChatMessageType type;

	@Column(name = "content", nullable = false, length = 2000)
	private String content;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	protected ChatMessage() {
	}

	private ChatMessage(UUID roomId, String senderId, long sequence, ChatMessageType type, String content, Instant createdAt) {
		this.id = UUID.randomUUID();
		this.roomId = roomId;
		this.senderId = senderId;
		this.sequence = sequence;
		this.type = type;
		this.content = content;
		this.createdAt = createdAt;
	}

	public static ChatMessage text(UUID roomId, String senderId, long sequence, String content, Instant createdAt) {
		return new ChatMessage(roomId, senderId, sequence, ChatMessageType.TEXT, content, createdAt);
	}

	public UUID getId() {
		return id;
	}

	public UUID getRoomId() {
		return roomId;
	}

	public String getSenderId() {
		return senderId;
	}

	public long getSequence() {
		return sequence;
	}

	public ChatMessageType getType() {
		return type;
	}

	public String getContent() {
		return content;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
