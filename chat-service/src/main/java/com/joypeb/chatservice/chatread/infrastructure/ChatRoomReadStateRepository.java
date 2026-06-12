package com.joypeb.chatservice.chatread.infrastructure;

import com.joypeb.chatservice.chatread.domain.ChatRoomReadState;
import com.joypeb.chatservice.chatread.domain.ChatRoomReadStateId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomReadStateRepository extends JpaRepository<ChatRoomReadState, ChatRoomReadStateId> {

	Optional<ChatRoomReadState> findByRoomIdAndMemberId(UUID roomId, String memberId);

	List<ChatRoomReadState> findByMemberIdAndRoomIdIn(String memberId, Collection<UUID> roomIds);

	@Modifying
	@Query(value = """
		insert into chat_room_read_states (room_id, member_id, last_read_sequence, created_at, updated_at)
		values (:roomId, :memberId, :lastReadSequence, current_timestamp, current_timestamp)
		on conflict (room_id, member_id) do update
		set last_read_sequence = greatest(chat_room_read_states.last_read_sequence, excluded.last_read_sequence),
		    updated_at = case
		        when excluded.last_read_sequence > chat_room_read_states.last_read_sequence then current_timestamp
		        else chat_room_read_states.updated_at
		    end
		""", nativeQuery = true)
	void upsertMonotonic(
		@Param("roomId") UUID roomId,
		@Param("memberId") String memberId,
		@Param("lastReadSequence") long lastReadSequence
	);
}
