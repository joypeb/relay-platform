package com.joypeb.chatservice.chatmessage.infrastructure;

import com.joypeb.chatservice.chatmessage.domain.ChatMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

	List<ChatMessage> findByRoomIdAndSequenceGreaterThanAndDeletedAtIsNullOrderBySequenceAsc(
		UUID roomId,
		long sequence,
		Pageable pageable
	);
}
