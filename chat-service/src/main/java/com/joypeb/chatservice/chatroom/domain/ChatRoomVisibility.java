package com.joypeb.chatservice.chatroom.domain;

public enum ChatRoomVisibility {
	PUBLIC,
	PRIVATE;

	public static ChatRoomVisibility fromPubliclyVisible(boolean publiclyVisible) {
		return publiclyVisible ? PUBLIC : PRIVATE;
	}
}
