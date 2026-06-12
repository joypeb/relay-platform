package com.joypeb.chatservice.chatread.application;

public class ChatReadSequenceOutOfRangeException extends RuntimeException {

	public ChatReadSequenceOutOfRangeException(long requested, long lastMessageSequence) {
		super("lastReadSequence " + requested + " is greater than room last message sequence " + lastMessageSequence + ".");
	}
}
