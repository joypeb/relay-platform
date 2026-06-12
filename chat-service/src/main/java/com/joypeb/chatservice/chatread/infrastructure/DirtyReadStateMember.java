package com.joypeb.chatservice.chatread.infrastructure;

import java.util.UUID;

public record DirtyReadStateMember(UUID roomId, String userId) {

	public static DirtyReadStateMember parse(String value) {
		int separator = value.indexOf(':');
		if (separator <= 0 || separator == value.length() - 1) {
			throw new IllegalArgumentException("Invalid dirty read state member.");
		}
		return new DirtyReadStateMember(UUID.fromString(value.substring(0, separator)), value.substring(separator + 1));
	}
}
