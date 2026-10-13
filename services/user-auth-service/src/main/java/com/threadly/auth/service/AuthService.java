package com.threadly.auth.service;

import com.threadly.auth.dto.*;

import java.util.UUID;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    /**
     * Find active user by Google email or create ROLE_USER with unusable password hash.
     */
    AuthResponse loginOrRegisterOAuth(String email, String displayName, String providerSubject);

    UserResponse getById(UUID id);

    /** Public profile card (email stripped); Redis-cached when enabled. */
    UserResponse getPublicById(UUID id);

    UserResponse getByUsername(String username);

    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);

    /**
     * Reputation adjustment from vote-service (karma on users).
     */
    UserResponse adjustKarma(UUID userId, long delta);

    void softDeleteAccount(UUID userId);
}
