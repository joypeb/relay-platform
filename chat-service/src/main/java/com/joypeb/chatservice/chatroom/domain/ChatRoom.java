package com.joypeb.chatservice.chatroom.domain;

import com.joypeb.chatservice.chatroom.application.ChatRoomForbiddenException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "chat_rooms")
public class ChatRoom {

	@Id
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "description", length = 500)
	private String description;

	@Column(name = "owner_id", nullable = false, length = 64)
	private String ownerId;

	@Enumerated(EnumType.STRING)
	@Column(name = "visibility", nullable = false, length = 20)
	private ChatRoomVisibility visibility;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ChatRoomStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	protected ChatRoom() {
	}

	private ChatRoom(
		String name,
		String description,
		String ownerId,
		ChatRoomVisibility visibility,
		Instant createdAt
	) {
		this.id = UUID.randomUUID();
		this.name = name;
		this.description = description;
		this.ownerId = ownerId;
		this.visibility = visibility;
		this.status = ChatRoomStatus.ACTIVE;
		this.createdAt = createdAt;
		this.updatedAt = createdAt;
	}

	public static ChatRoom create(
		String name,
		String description,
		String ownerId,
		ChatRoomVisibility visibility,
		Instant createdAt
	) {
		return new ChatRoom(name, description, ownerId, visibility, createdAt);
	}

	public void update(
		String actorId,
		String name,
		String description,
		ChatRoomVisibility visibility,
		Instant updatedAt
	) {
		requireOwner(actorId);
		requireActive();
		this.name = name;
		this.description = description;
		this.visibility = visibility;
		this.updatedAt = updatedAt;
	}

	public void delete(String actorId, Instant deletedAt) {
		requireOwner(actorId);
		requireActive();
		this.status = ChatRoomStatus.DELETED;
		this.deletedAt = deletedAt;
		this.updatedAt = deletedAt;
	}

	private void requireOwner(String actorId) {
		if (!Objects.equals(ownerId, actorId)) {
			throw new ChatRoomForbiddenException();
		}
	}

	private void requireActive() {
		if (status != ChatRoomStatus.ACTIVE || deletedAt != null) {
			throw new ChatRoomForbiddenException();
		}
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

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public ChatRoomVisibility getVisibility() {
		return visibility;
	}

	public ChatRoomStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
