package com.joypeb.chatservice.chatread.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ChatRoomReadStateId implements Serializable {

	private UUID roomId;
	private String memberId;

	protected ChatRoomReadStateId() {
	}

	public ChatRoomReadStateId(UUID roomId, String memberId) {
		this.roomId = roomId;
		this.memberId = memberId;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof ChatRoomReadStateId that)) {
			return false;
		}
		return Objects.equals(roomId, that.roomId) && Objects.equals(memberId, that.memberId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(roomId, memberId);
	}
}
