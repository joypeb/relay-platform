package com.joypeb.chatservice.chatroom.api;

import com.joypeb.chatservice.chatroom.application.ChatRoomService;
import com.joypeb.chatservice.chatroom.dto.ChatRoomCreateRequest;
import com.joypeb.chatservice.chatroom.dto.ChatRoomResponse;
import com.joypeb.chatservice.chatroom.dto.ChatRoomSummaryResponse;
import com.joypeb.chatservice.chatroom.dto.ChatRoomUpdateRequest;
import com.joypeb.chatservice.common.api.ApiResponse;
import com.joypeb.chatservice.common.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/chat-rooms")
public class ChatRoomController {

	private static final String ACTOR_HEADER = "X-User-Id";

	private final ChatRoomService chatRoomService;

	public ChatRoomController(ChatRoomService chatRoomService) {
		this.chatRoomService = chatRoomService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<ChatRoomResponse>> create(
		@RequestHeader(ACTOR_HEADER) @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{1,64}$") String actorId,
		@Valid @RequestBody ChatRoomCreateRequest request
	) {
		ChatRoomResponse response = chatRoomService.create(actorId, request);
		return ResponseEntity.created(URI.create("/api/v1/chat-rooms/" + response.id()))
			.body(ApiResponse.success(response));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<ChatRoomSummaryResponse>>> list(
		@RequestHeader(ACTOR_HEADER) @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{1,64}$") String actorId,
		@RequestParam(defaultValue = "public") ChatRoomListScope scope,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
	) {
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		return ResponseEntity.ok(ApiResponse.success(chatRoomService.list(actorId, scope, pageRequest)));
	}

	@GetMapping("/{roomId}")
	public ResponseEntity<ApiResponse<ChatRoomResponse>> get(
		@RequestHeader(ACTOR_HEADER) @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{1,64}$") String actorId,
		@PathVariable UUID roomId
	) {
		return ResponseEntity.ok(ApiResponse.success(chatRoomService.get(actorId, roomId)));
	}

	@PatchMapping("/{roomId}")
	public ResponseEntity<ApiResponse<ChatRoomResponse>> update(
		@RequestHeader(ACTOR_HEADER) @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{1,64}$") String actorId,
		@PathVariable UUID roomId,
		@Valid @RequestBody ChatRoomUpdateRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success(chatRoomService.update(actorId, roomId, request)));
	}

	@DeleteMapping("/{roomId}")
	public ResponseEntity<Void> delete(
		@RequestHeader(ACTOR_HEADER) @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{1,64}$") String actorId,
		@PathVariable UUID roomId
	) {
		chatRoomService.delete(actorId, roomId);
		return ResponseEntity.noContent().build();
	}

}
