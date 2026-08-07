package com.threadly.auth.security;

import java.util.List;

/**
 * Port for token creation/validation.
 * Keeps domain use-cases independent of jjwt concrete types.
 */
public interface JwtTokenProvider {

    String generateToken(Long userId, String username, List<String> roles, List<String> permissions);

    boolean isValid(String token);

    /** User id from JWT subject (string form). */
    String extractUserId(String token);

    /** Username claim (may be null for older tokens). */
    String extractUsername(String token);

    List<String> extractRoles(String token);

    List<String> extractPermissions(String token);
}
