package com.joypeb.chatservice.chatmessage.application;

public class ChatMessageContentInvalidException extends RuntimeException {

	public ChatMessageContentInvalidException() {
		super("Chat message content is invalid.");
	}
}
