package com.threadly.auth.security;

import io.jsonwebtoken.Claims;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public interface JwtTokenProvider {

    String generateToken(UUID userId, String username, List<String> roles, List<String> permissions);

    boolean isValid(String token);

    String extractUserId(String token);

    String extractUsername(String token);

    List<String> extractRoles(String token);

    List<String> extractPermissions(String token);

    Claims parseClaims(String token);

    String extractJti(String token);

    /** Remaining lifetime until exp; empty/zero if already expired. */
    Duration remainingTtl(String token);
}
