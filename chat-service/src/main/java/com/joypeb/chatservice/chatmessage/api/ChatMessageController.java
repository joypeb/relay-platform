package com.joypeb.chatservice.chatmessage.api;

import com.joypeb.chatservice.chatmessage.application.ChatMessageService;
import com.joypeb.chatservice.chatmessage.dto.ChatMessageResponse;
import com.joypeb.chatservice.chatmessage.dto.ChatMessageSendRequest;
import com.joypeb.chatservice.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping
public class ChatMessageController {

	private static final String ACTOR_HEADER = "X-User-Id";

	private final ChatMessageService chatMessageService;

	public ChatMessageController(ChatMessageService chatMessageService) {
		this.chatMessageService = chatMessageService;
	}

	@PostMapping("/internal/chat-rooms/{roomId}/messages")
	public ResponseEntity<ApiResponse<ChatMessageResponse>> send(
		@RequestHeader(ACTOR_HEADER) @Pattern(regexp = "^[A-Za-z0-9._-]{1,64}$") String actorId,
		@PathVariable UUID roomId,
		@Valid @RequestBody ChatMessageSendRequest request
	) {
		ChatMessageResponse response = chatMessageService.send(actorId, roomId, request);
		return ResponseEntity
			.created(URI.create("/api/v1/chat-rooms/" + roomId + "/messages/" + response.id()))
			.body(ApiResponse.success(response));
	}

	@GetMapping("/api/v1/chat-rooms/{roomId}/messages")
	public ApiResponse<List<ChatMessageResponse>> history(
		@RequestHeader(ACTOR_HEADER) @Pattern(regexp = "^[A-Za-z0-9._-]{1,64}$") String actorId,
		@PathVariable UUID roomId,
		@RequestParam(defaultValue = "0") @Min(0) long afterSequence,
		@RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
	) {
		return ApiResponse.success(chatMessageService.history(actorId, roomId, afterSequence, size));
	}
}
