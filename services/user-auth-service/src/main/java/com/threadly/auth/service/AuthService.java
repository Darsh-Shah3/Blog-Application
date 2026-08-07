package com.threadly.auth.service;

import com.threadly.auth.dto.*;

/**
 * Auth use-cases exposed to the web layer.
 * Controllers depend on this contract only — never on {@code AuthServiceImpl}.
 */
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getById(Long id);

    UserResponse getByUsername(String username);

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * Side-effect from vote-service; kept on the auth boundary so karma ownership stays in this service.
     */
    UserResponse adjustKarma(Long userId, long delta);

    /** Soft-delete the caller's own account (username/email free for re-registration after). */
    void softDeleteAccount(Long userId);
}
