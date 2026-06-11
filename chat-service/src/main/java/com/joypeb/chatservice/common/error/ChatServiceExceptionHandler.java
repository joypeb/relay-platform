package com.joypeb.chatservice.common.error;

import com.joypeb.chatservice.chatmessage.application.ChatMessageContentInvalidException;
import com.joypeb.chatservice.chatroom.application.ChatRoomMemberAlreadyExistsException;
import com.joypeb.chatservice.chatroom.application.ChatRoomForbiddenException;
import com.joypeb.chatservice.chatroom.application.ChatRoomNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ChatServiceExceptionHandler {

	@ExceptionHandler(ChatRoomNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleNotFound(ChatRoomNotFoundException exception, HttpServletRequest request) {
		ProblemDetail problem = problem(HttpStatus.NOT_FOUND, "Chat room not found", exception.getMessage(), request);
		problem.setType(URI.create("https://joypeb.com/problems/chat-room-not-found"));
		problem.setProperty("code", "CHAT_ROOM_NOT_FOUND");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
	}

	@ExceptionHandler(ChatRoomForbiddenException.class)
	public ResponseEntity<ProblemDetail> handleForbidden(ChatRoomForbiddenException exception, HttpServletRequest request) {
		ProblemDetail problem = problem(HttpStatus.FORBIDDEN, "Chat room forbidden", exception.getMessage(), request);
		problem.setType(URI.create("https://joypeb.com/problems/chat-room-forbidden"));
		problem.setProperty("code", "CHAT_ROOM_FORBIDDEN");
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
	}

	@ExceptionHandler(ChatRoomMemberAlreadyExistsException.class)
	public ResponseEntity<ProblemDetail> handleMemberAlreadyExists(
		ChatRoomMemberAlreadyExistsException exception,
		HttpServletRequest request
	) {
		ProblemDetail problem = problem(HttpStatus.CONFLICT, "Chat room member already exists", exception.getMessage(), request);
		problem.setType(URI.create("https://joypeb.com/problems/chat-room-member-already-exists"));
		problem.setProperty("code", "CHAT_ROOM_MEMBER_ALREADY_EXISTS");
		return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
	}

	@ExceptionHandler(ChatMessageContentInvalidException.class)
	public ResponseEntity<ProblemDetail> handleInvalidMessageContent(
		ChatMessageContentInvalidException exception,
		HttpServletRequest request
	) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Chat message content invalid", exception.getMessage(), request);
		problem.setType(URI.create("https://joypeb.com/problems/chat-message-content-invalid"));
		problem.setProperty("code", "CHAT_MESSAGE_CONTENT_INVALID");
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler({
		MethodArgumentNotValidException.class,
		ConstraintViolationException.class,
		MissingRequestHeaderException.class,
		MethodArgumentTypeMismatchException.class
	})
	public ResponseEntity<ProblemDetail> handleBadRequest(Exception exception, HttpServletRequest request) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Request validation failed", "Request is invalid.", request);
		problem.setType(URI.create("https://joypeb.com/problems/request-validation-failed"));
		problem.setProperty("code", "REQUEST_VALIDATION_FAILED");
		return ResponseEntity.badRequest().body(problem);
	}

	private ProblemDetail problem(HttpStatus status, String title, String detail, HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setInstance(URI.create(request.getRequestURI()));
		return problem;
	}
}
