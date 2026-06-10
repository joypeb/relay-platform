package com.joypeb.gateway.session;

import java.security.Principal;
import java.util.Objects;

public final class GatewayUserPrincipal implements Principal {

	private final String userId;

	public GatewayUserPrincipal(String userId) {
		this.userId = Objects.requireNonNull(userId, "userId");
	}

	@Override
	public String getName() {
		return userId;
	}
}
