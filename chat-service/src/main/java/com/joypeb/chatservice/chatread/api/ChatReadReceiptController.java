package com.joypeb.chatservice.chatread.api;

import com.joypeb.chatservice.chatread.application.ChatReadStateService;
import com.joypeb.chatservice.chatread.dto.ChatReadReceiptRequest;
import com.joypeb.chatservice.chatread.dto.ChatReadReceiptResponse;
import com.joypeb.chatservice.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/internal/chat-rooms/{roomId}/read-receipts")
public class ChatReadReceiptController {

	private static final String ACTOR_HEADER = "X-User-Id";

	private final ChatReadStateService chatReadStateService;

	public ChatReadReceiptController(ChatReadStateService chatReadStateService) {
		this.chatReadStateService = chatReadStateService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<ChatReadReceiptResponse>> accept(
		@RequestHeader(ACTOR_HEADER) @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{1,64}$") String actorId,
		@PathVariable UUID roomId,
		@Valid @RequestBody ChatReadReceiptRequest request
	) {
		ChatReadReceiptResponse response = chatReadStateService.acceptReadReceipt(
			actorId,
			roomId,
			request.requestId(),
			request.payload().lastReadSequence()
		);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(response));
	}
}
