package com.joypeb.chatservice.chatmessage.infrastructure;

import com.joypeb.chatservice.chatmessage.domain.ChatMessageSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ChatMessageSequenceRepository extends JpaRepository<ChatMessageSequence, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ChatMessageSequence> findWithLockByRoomId(UUID roomId);
}
