package com.joypeb.chatservice.chatread.infrastructure;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ChatReadStateStore {

	Optional<Long> findRoomLastSequence(UUID roomId);

	void warmRoomLastSequence(UUID roomId, long sequence);

	boolean updateRoomLastSequence(UUID roomId, long sequence);

	Optional<Long> findUserReadSequence(String userId, UUID roomId);

	void warmUserReadSequence(String userId, UUID roomId, long sequence);

	boolean advanceUserReadSequence(String userId, UUID roomId, long sequence);

	Set<String> dirtyMembers(int limit);

	boolean removeDirtyIfSequenceUnchanged(String member, long flushedSequence);
}
