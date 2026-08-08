package com.threadly.community.security;

import java.security.Principal;
import java.util.UUID;

/** STOMP principal carrying the JWT subject and username. */
public record ChatPrincipal(UUID userId, String username) implements Principal {
    @Override
    public String getName() {
        return userId != null ? userId.toString() : "anonymous";
    }
}
