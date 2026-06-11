package com.joypeb.chatservice.chatroom.application;

import java.util.UUID;

public class ChatRoomMemberAlreadyExistsException extends RuntimeException {

	private final UUID roomId;
	private final String memberId;

	public ChatRoomMemberAlreadyExistsException(UUID roomId, String memberId) {
		super("Chat room member already exists.");
		this.roomId = roomId;
		this.memberId = memberId;
	}

	public UUID roomId() {
		return roomId;
	}

	public String memberId() {
		return memberId;
	}
}
