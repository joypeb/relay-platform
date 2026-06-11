package com.joypeb.chatservice.chatroom.application;

import java.util.UUID;

public class ChatRoomNotFoundException extends RuntimeException {

	private final UUID roomId;

	public ChatRoomNotFoundException(UUID roomId) {
		super("Chat room was not found.");
		this.roomId = roomId;
	}

	public UUID roomId() {
		return roomId;
	}
}
