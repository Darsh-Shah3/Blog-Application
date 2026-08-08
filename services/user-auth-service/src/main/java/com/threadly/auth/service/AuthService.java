package com.threadly.auth.service;

import com.threadly.auth.dto.*;

import java.util.UUID;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getById(UUID id);

    UserResponse getByUsername(String username);

    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);

    /**
     * Reputation adjustment from vote-service (karma on users).
     */
    UserResponse adjustKarma(UUID userId, long delta);

    void softDeleteAccount(UUID userId);
}
