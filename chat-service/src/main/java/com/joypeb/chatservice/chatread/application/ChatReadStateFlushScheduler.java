package com.joypeb.chatservice.chatread.application;

import com.joypeb.chatservice.chatread.config.ChatReadStateRedisProperties;
import com.joypeb.chatservice.chatread.infrastructure.ChatReadStateStore;
import com.joypeb.chatservice.chatread.infrastructure.DirtyReadStateMember;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ChatReadStateFlushScheduler {

	private static final Logger log = LoggerFactory.getLogger(ChatReadStateFlushScheduler.class);

	private final ChatReadStateStore readStateStore;
	private final ChatReadStateService chatReadStateService;
	private final ChatReadStateRedisProperties properties;

	public ChatReadStateFlushScheduler(
		ChatReadStateStore readStateStore,
		ChatReadStateService chatReadStateService,
		ChatReadStateRedisProperties properties
	) {
		this.readStateStore = readStateStore;
		this.chatReadStateService = chatReadStateService;
		this.properties = properties;
	}

	@Scheduled(fixedDelayString = "${chat.read-state.flush-interval-ms:30000}")
	public void flushDirtyReadStates() {
		Set<String> members = readStateStore.dirtyMembers(properties.flushBatchSize());
		int flushed = 0;
		int failed = 0;
		for (String member : members) {
			try {
				DirtyReadStateMember parsed = DirtyReadStateMember.parse(member);
				long sequence = readStateStore.findUserReadSequence(parsed.userId(), parsed.roomId()).orElse(0L);
				chatReadStateService.saveMonotonic(parsed.userId(), parsed.roomId(), sequence);
				readStateStore.removeDirtyIfSequenceUnchanged(member, sequence);
				flushed += 1;
			}
			catch (RuntimeException exception) {
				failed += 1;
				log.warn("Failed to flush chat read state dirty member.", exception);
			}
		}
		if (!members.isEmpty()) {
			log.info("Flushed chat read states. candidates={}, success={}, failed={}", members.size(), flushed, failed);
		}
	}
}
