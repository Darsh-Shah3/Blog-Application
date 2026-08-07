package com.threadly.auth.service.impl;

import com.threadly.auth.dto.*;
import com.threadly.auth.entity.Role;
import com.threadly.auth.entity.User;
import com.threadly.auth.exception.ApiException;
import com.threadly.auth.mapper.UserMapper;
import com.threadly.auth.rbac.RbacCatalog;
import com.threadly.auth.repository.RoleRepository;
import com.threadly.auth.repository.UserRepository;
import com.threadly.auth.security.JwtTokenProvider;
import com.threadly.auth.service.AuthService;
import com.threadly.auth.util.AuditActors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;

/**
 * Default auth use-case orchestrator.
 * Depends only on repository abstractions, password hashing port, JWT port, and mapper —
 * so infrastructure can be swapped without touching API controllers.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Register attempt username={} email={}", request.getUsername(), request.getEmail());
        if (userRepository.existsActiveByEmail(request.getEmail())) {
            log.warn("Register failed email already registered email={}", request.getEmail());
            throw new ApiException("Email already registered", HttpStatus.CONFLICT.value());
        }
        if (userRepository.existsActiveByUsername(request.getUsername())) {
            log.warn("Register failed username taken username={}", request.getUsername());
            throw new ApiException("Username already taken", HttpStatus.CONFLICT.value());
        }
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ApiException("Default role missing", 500));

        String username = request.getUsername().toLowerCase();
        User user = User.builder()
                .username(username)
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName() == null || request.getDisplayName().isBlank()
                        ? request.getUsername() : request.getDisplayName())
                .bio(null)
                .karma(0L)
                .createdBy(username)
                .updatedBy(username)
                .build();
        user.getRoles().add(userRole);
        user = userRepository.save(user);
        log.info("User registered id={} username={}", user.getId(), user.getUsername());
        return toAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt principal={}", request.getEmailOrUsername());
        String key = request.getEmailOrUsername().toLowerCase();
        User user = userRepository.findActiveByEmail(key)
                .or(() -> userRepository.findActiveByUsername(key))
                .orElseThrow(() -> {
                    log.warn("Login failed user not found principal={}", request.getEmailOrUsername());
                    return new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED.value());
                });
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed bad password userId={}", user.getId());
            throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED.value());
        }
        log.info("Login success userId={} username={}", user.getId(), user.getUsername());
        return toAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        log.debug("Get user by id={}", id);
        return userMapper.toResponse(findActiveUser(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getByUsername(String username) {
        log.debug("Get user by username={}", username);
        User user = userRepository.findActiveByUsername(username.toLowerCase())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND.value()));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        log.info("Update profile userId={}", userId);
        User user = findActiveUser(userId);
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        user.setUpdatedBy(user.getUsername());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse adjustKarma(Long userId, long delta) {
        log.info("Adjust karma userId={} delta={}", userId, delta);
        User user = findActiveUser(userId);
        user.setKarma(user.getKarma() + delta);
        user.setUpdatedBy("system");
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void softDeleteAccount(Long userId) {
        User user = findActiveUser(userId);
        softDelete(user, user.getUsername());
        log.info("User soft-deleted self id={} username={}", userId, user.getUsername());
    }

    private void softDelete(User user, String actorUsername) {
        user.setDeletedAt(Instant.now());
        user.setUpdatedBy(AuditActors.resolve(actorUsername, user.getId()));
        userRepository.save(user);
    }

    private User findActiveUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found id={}", id);
                    return new ApiException("User not found", HttpStatus.NOT_FOUND.value());
                });
        if (user.isDeleted()) {
            log.warn("Soft-deleted user access id={}", id);
            throw new ApiException("User not found", HttpStatus.NOT_FOUND.value());
        }
        return user;
    }

    private AuthResponse toAuthResponse(User user) {
        var roles = user.getRoles().stream().map(Role::getName).toList();
        var permissions = new ArrayList<>(RbacCatalog.permissionNamesForRoles(roles));
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), roles, permissions);
        log.debug("Issued JWT userId={} roles={} permissions={}", user.getId(), roles, permissions);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(userMapper.toResponse(user))
                .build();
    }
}
