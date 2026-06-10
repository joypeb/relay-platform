package com.joypeb.chatservice.chatroom.application;

public class ChatRoomForbiddenException extends RuntimeException {

	public ChatRoomForbiddenException() {
		super("Only the chat room owner can change this chat room.");
	}
}
