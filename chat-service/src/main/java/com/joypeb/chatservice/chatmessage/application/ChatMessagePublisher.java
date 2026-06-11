package com.joypeb.chatservice.chatmessage.application;

import com.joypeb.chatservice.chatmessage.event.ChatMessageCreatedEvent;

public interface ChatMessagePublisher {

	void publishCreated(ChatMessageCreatedEvent event);
}
