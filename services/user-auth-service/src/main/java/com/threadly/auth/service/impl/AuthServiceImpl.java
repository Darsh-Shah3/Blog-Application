package com.threadly.auth.service.impl;

import com.threadly.auth.dto.*;
import com.threadly.auth.entity.Role;
import com.threadly.auth.entity.User;
import com.threadly.auth.entity.UserFollow;
import com.threadly.auth.exception.ApiException;
import com.threadly.auth.mapper.UserMapper;
import com.threadly.auth.rbac.RbacCatalog;
import com.threadly.auth.repository.RoleRepository;
import com.threadly.auth.repository.UserFollowRepository;
import com.threadly.auth.repository.UserRepository;
import com.threadly.auth.cache.RedisJsonCache;
import com.threadly.auth.security.JwtTokenProvider;
import com.threadly.auth.service.AuthService;
import com.threadly.auth.service.CaptchaService;
import com.threadly.auth.service.TokenBlacklistService;
import com.threadly.auth.util.AuditActors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final Duration PROFILE_TTL = Duration.ofMinutes(3);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserFollowRepository followRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final CaptchaService captchaService;
    private final TokenBlacklistService tokenBlacklistService;
    private final ObjectProvider<RedisJsonCache> cacheProvider;

    @Value("${app.cache.redis-enabled:false}")
    private boolean cacheEnabled;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Register attempt username={} email={}", request.getUsername(), request.getEmail());
        captchaService.verifySignupToken(request.getCaptchaToken(), clientIp());
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
        user.addRole(userRole);
        user = userRepository.save(user);
        ensureSocialRow(user);
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
    @Transactional
    public AuthResponse loginOrRegisterOAuth(String email, String displayName, String providerSubject) {
        String emailKey = email == null ? "" : email.trim().toLowerCase();
        if (emailKey.isBlank() || !emailKey.contains("@")) {
            throw new ApiException("Invalid OAuth email", HttpStatus.BAD_REQUEST.value());
        }
        var existing = userRepository.findActiveByEmail(emailKey);
        if (existing.isPresent()) {
            User user = existing.get();
            log.info("OAuth login existing userId={} username={} subject={}",
                    user.getId(), user.getUsername(), providerSubject);
            return toAuthResponse(user);
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ApiException("Default role missing", 500));
        String username = allocateUsername(emailKey);
        String name = displayName == null || displayName.isBlank() ? username : displayName.trim();
        if (name.length() > 100) {
            name = name.substring(0, 100);
        }
        // Unusable password — account is OAuth-only unless they later use password reset (if allowed)
        String passwordHash = passwordEncoder.encode(UUID.randomUUID() + ":" + UUID.randomUUID());
        User user = User.builder()
                .username(username)
                .email(emailKey)
                .passwordHash(passwordHash)
                .displayName(name)
                .bio(null)
                .karma(0L)
                .createdBy(username)
                .updatedBy(username)
                .build();
        user.addRole(userRole);
        user = userRepository.save(user);
        ensureSocialRow(user);
        log.info("OAuth registered userId={} username={} email={} subject={}",
                user.getId(), user.getUsername(), user.getEmail(), providerSubject);
        return toAuthResponse(user);
    }

    /** Derive a unique username from the email local-part. */
    private String allocateUsername(String email) {
        String local = email.substring(0, email.indexOf('@')).toLowerCase();
        String base = local.replaceAll("[^a-z0-9_]", "");
        if (base.length() < 3) {
            base = ("user" + base).replaceAll("[^a-z0-9_]", "");
        }
        if (base.length() < 3) {
            base = "user";
        }
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }
        String candidate = base;
        int n = 0;
        while (userRepository.existsActiveByUsername(candidate)) {
            n++;
            String suffix = "_" + n;
            int maxBase = Math.max(1, 50 - suffix.length());
            candidate = base.substring(0, Math.min(base.length(), maxBase)) + suffix;
        }
        return candidate;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        log.debug("Get user by id={}", id);
        return userMapper.toResponse(findActiveUser(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getPublicById(UUID id) {
        RedisJsonCache cache = cache();
        if (cache != null) {
            var hit = cache.get(profileIdKey(id), UserResponse.class);
            if (hit.isPresent()) {
                return hit.get();
            }
        }
        UserResponse response = publicCard(userMapper.toResponse(findActiveUser(id)));
        putProfileCache(response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getByUsername(String username) {
        log.debug("Get user by username={}", username);
        String key = username == null ? "" : username.trim().toLowerCase();
        RedisJsonCache cache = cache();
        if (cache != null && !key.isBlank()) {
            var hit = cache.get(profileUsernameKey(key), UserResponse.class);
            if (hit.isPresent()) {
                return hit.get();
            }
        }
        UserResponse response = userRepository.findActiveByUsername(key)
                .map(userMapper::toResponse)
                .map(this::publicCard)
                .orElseGet(() -> userRepository
                        .findDeletedByUsername(key, org.springframework.data.domain.PageRequest.of(0, 1))
                        .stream()
                        .findFirst()
                        .map(userMapper::toDeletedPublicCard)
                        .map(this::publicCard)
                        .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND.value())));
        putProfileCache(response);
        return response;
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        log.info("Update profile userId={}", userId);
        User user = findActiveUser(userId);
        String oldUsername = user.getUsername();
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getAvatarMediaId() != null) {
            String raw = request.getAvatarMediaId().trim();
            if (raw.isEmpty()) {
                user.setAvatarMediaId(null);
            } else {
                try {
                    user.setAvatarMediaId(UUID.fromString(raw));
                } catch (IllegalArgumentException ex) {
                    throw new ApiException("Invalid avatar media id", HttpStatus.BAD_REQUEST.value());
                }
            }
        }
        user.setUpdatedBy(user.getUsername());
        User saved = userRepository.save(user);
        invalidateProfileCache(userId, oldUsername);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse adjustKarma(UUID userId, long delta) {
        log.info("Adjust karma userId={} delta={}", userId, delta);
        User user = findActiveUser(userId);
        user.setKarma(user.getKarma() + delta);
        user.setUpdatedBy("system");
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void softDeleteAccount(UUID userId) {
        User user = findActiveUser(userId);
        softDelete(user, user.getUsername());
        log.info("User soft-deleted self id={} username={}", userId, user.getUsername());
    }

    private void softDelete(User user, String actorUsername) {
        user.setDeletedAt(Instant.now());
        user.setUpdatedBy(AuditActors.resolve(actorUsername, user.getId()));
        userRepository.save(user);
        invalidateProfileCache(user.getId(), user.getUsername());
        tokenBlacklistService.revokeAllForUser(user.getId());
    }

    private RedisJsonCache cache() {
        return cacheEnabled ? cacheProvider.getIfAvailable() : null;
    }

    private void putProfileCache(UserResponse response) {
        RedisJsonCache cache = cache();
        if (cache == null || response == null || response.getId() == null) {
            return;
        }
        UserResponse card = publicCard(response);
        if (response.getUsername() != null) {
            cache.set(profileUsernameKey(response.getUsername()), card, PROFILE_TTL);
        }
        cache.set(profileIdKey(response.getId()), card, PROFILE_TTL);
    }

    private void invalidateProfileCache(UUID userId, String username) {
        RedisJsonCache cache = cache();
        if (cache == null) {
            return;
        }
        if (userId != null) {
            cache.delete(profileIdKey(userId));
        }
        if (username != null && !username.isBlank()) {
            cache.delete(profileUsernameKey(username));
        }
    }

    private UserResponse publicCard(UserResponse user) {
        if (user != null) {
            user.setEmail(null);
        }
        return user;
    }

    private static String profileUsernameKey(String username) {
        return "user:profile:v1:u:" + username.trim().toLowerCase();
    }

    private static String profileIdKey(UUID id) {
        return "user:profile:v1:id:" + id;
    }

    private void ensureSocialRow(User user) {
        if (followRepository.findByUserId(user.getId()).isEmpty()) {
            followRepository.save(UserFollow.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .build());
        }
    }

    private User findActiveUser(UUID id) {
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

    private static String clientIp() {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (!(attrs instanceof ServletRequestAttributes sra)) {
                return null;
            }
            var request = sra.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception ignored) {
            return null;
        }
    }
}
