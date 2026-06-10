package com.joypeb.chatservice.chatroom.infrastructure;

import com.joypeb.chatservice.chatroom.domain.ChatRoomMember;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, UUID> {

	long countByRoomIdAndLeftAtIsNull(UUID roomId);
}
