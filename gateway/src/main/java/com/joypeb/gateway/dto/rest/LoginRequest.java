package com.joypeb.gateway.dto.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank
		@Size(max = 64)
		@Pattern(regexp = "^[A-Za-z0-9._-]+$")
		String userId
) {
}
