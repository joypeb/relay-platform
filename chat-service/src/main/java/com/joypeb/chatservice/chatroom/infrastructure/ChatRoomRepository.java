package com.joypeb.chatservice.chatroom.infrastructure;

import com.joypeb.chatservice.chatroom.domain.ChatRoom;
import com.joypeb.chatservice.chatroom.domain.ChatRoomVisibility;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

	Optional<ChatRoom> findByIdAndDeletedAtIsNull(UUID id);

	Page<ChatRoom> findByDeletedAtIsNullAndVisibility(ChatRoomVisibility visibility, Pageable pageable);

	@Query("""
		select room
		from ChatRoom room
		join ChatRoomMember member on member.roomId = room.id
		where room.deletedAt is null
		  and member.leftAt is null
		  and member.memberId = :memberId
		""")
	Page<ChatRoom> findJoinedRooms(@Param("memberId") String memberId, Pageable pageable);
}
