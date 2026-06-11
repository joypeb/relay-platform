package com.joypeb.chatservice.chatroom.api;

import java.util.Locale;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ChatRoomListScopeConverter implements Converter<String, ChatRoomListScope> {

	@Override
	public ChatRoomListScope convert(String source) {
		return ChatRoomListScope.valueOf(source.trim().toUpperCase(Locale.ROOT));
	}
}
