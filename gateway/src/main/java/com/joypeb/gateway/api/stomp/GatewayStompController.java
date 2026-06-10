package com.joypeb.gateway.api.stomp;

import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import com.joypeb.gateway.dto.stomp.StompAckRequest;
import com.joypeb.gateway.dto.stomp.StompServerEvent;

import jakarta.validation.Valid;

@Validated
@Controller
public class GatewayStompController {

	private final Clock clock;

	public GatewayStompController(Clock clock) {
		this.clock = clock;
	}

	@MessageMapping("/gateway/acks")
	@SendToUser("/queue/gateway/acks")
	public StompServerEvent<Map<String, String>> acknowledge(
			@Valid @Payload StompAckRequest request,
			Principal principal
	) {
		return new StompServerEvent<>(
				UUID.randomUUID().toString(),
				"GATEWAY_ACK_CREATED",
				Map.of(
						"requestId", request.requestId(),
						"userId", principal.getName()
				),
				Instant.now(clock),
				UUID.randomUUID().toString()
		);
	}
}
