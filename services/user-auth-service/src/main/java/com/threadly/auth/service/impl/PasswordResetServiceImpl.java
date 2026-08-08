package com.threadly.auth.service.impl;

import com.threadly.auth.config.MailAppProperties;
import com.threadly.auth.dto.ForgotPasswordRequest;
import com.threadly.auth.dto.MessageResponse;
import com.threadly.auth.dto.ResetPasswordRequest;
import com.threadly.auth.entity.PasswordResetRateLimit;
import com.threadly.auth.entity.PasswordResetToken;
import com.threadly.auth.entity.User;
import com.threadly.auth.exception.ApiException;
import com.threadly.auth.repository.PasswordResetRateLimitRepository;
import com.threadly.auth.repository.PasswordResetTokenRepository;
import com.threadly.auth.repository.UserRepository;
import com.threadly.auth.service.MailService;
import com.threadly.auth.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_ATTEMPTS_PER_DAY = 3;
    private static final long LOCKOUT_SECONDS = 3600L;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetRateLimitRepository rateLimitRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final MailAppProperties mailProps;

    /**
     * Always returns the same generic message to avoid email enumeration
     * (except rate-limit after identity is known).
     */
    @Override
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String generic = "If that email is registered, a reset link was sent.";

        Optional<User> userOpt = userRepository.findActiveByEmail(email);
        if (userOpt.isEmpty()) {
            log.info("Forgot-password for unknown email (no-op)");
            return MessageResponse.builder().message(generic).build();
        }

        User user = userOpt.get();
        enforceAndRecordAttempt(user);

        tokenRepository.deleteUnusedForUser(user.getId());

        String rawToken = generateRawToken();
        Instant expires = Instant.now().plusSeconds(mailProps.getResetTokenMinutes() * 60);

        tokenRepository.save(PasswordResetToken.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .tokenHash(sha256(rawToken))
                .expiresAt(expires)
                .used(false)
                .build());

        String base = mailProps.getFrontendBaseUrl().replaceAll("/$", "");
        String resetUrl = base + "/reset-password?token=" + rawToken;

        try {
            mailService.sendPasswordReset(user.getEmail(), resetUrl);
        } catch (Exception ex) {
            throw new ApiException(
                    "Could not send email. Check SMTP settings.",
                    HttpStatus.SERVICE_UNAVAILABLE.value());
        }

        log.info("Password reset issued userId={} username={} expiresAt={}", user.getId(), user.getUsername(), expires);
        return MessageResponse.builder().message(generic).build();
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String hash = sha256(request.getToken().trim());
        PasswordResetToken token = tokenRepository.findByTokenHashAndUsedFalse(hash)
                .orElseThrow(() -> new ApiException(
                        "Invalid or expired reset link. Request a new one.",
                        HttpStatus.BAD_REQUEST.value()));

        if (token.isUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(
                    "Reset link expired. Request a new one.",
                    HttpStatus.BAD_REQUEST.value());
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.BAD_REQUEST.value()));
        if (user.isDeleted()) {
            throw new ApiException("Invalid or expired reset link. Request a new one.", HttpStatus.BAD_REQUEST.value());
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedBy(user.getUsername());
        userRepository.save(user);

        token.setUsed(true);
        token.setUsedAt(Instant.now());
        tokenRepository.save(token);
        tokenRepository.deleteUnusedForUser(user.getId());

        log.info("Password reset completed userId={} username={}", user.getId(), user.getUsername());
        return MessageResponse.builder().message("Password updated. You can sign in now.").build();
    }

    /**
     * Max 3 forgot-password attempts per calendar day (UTC). After the 3rd is used up,
     * user must wait 1 hour before trying again.
     */
    private void enforceAndRecordAttempt(User user) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant now = Instant.now();

        PasswordResetRateLimit limit = rateLimitRepository.findById(user.getId())
                .orElseGet(() -> PasswordResetRateLimit.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .windowDate(today)
                        .attemptCount(0)
                        .build());

        if (limit.getLockedUntil() != null && limit.getLockedUntil().isAfter(now)) {
            throw new ApiException(
                    "Too many password reset attempts. Try again after 1 hour.",
                    HttpStatus.TOO_MANY_REQUESTS.value());
        }

        // New day → reset the daily counter (lock already expired or none)
        if (!today.equals(limit.getWindowDate())) {
            limit.setWindowDate(today);
            limit.setAttemptCount(0);
            limit.setLockedUntil(null);
        }

        if (limit.getAttemptCount() >= MAX_ATTEMPTS_PER_DAY) {
            // Day quota spent and no active lock — open with a fresh 1h lock after 3rd was already spent
            if (limit.getLockedUntil() == null || !limit.getLockedUntil().isAfter(now)) {
                // allow if lock expired even same day
                limit.setAttemptCount(0);
                limit.setLockedUntil(null);
            }
        }

        if (limit.getAttemptCount() >= MAX_ATTEMPTS_PER_DAY) {
            throw new ApiException(
                    "Too many password reset attempts. Try again after 1 hour.",
                    HttpStatus.TOO_MANY_REQUESTS.value());
        }

        limit.setAttemptCount(limit.getAttemptCount() + 1);
        limit.setUsername(user.getUsername());
        limit.setUpdatedAt(now);
        if (limit.getAttemptCount() >= MAX_ATTEMPTS_PER_DAY) {
            limit.setLockedUntil(now.plusSeconds(LOCKOUT_SECONDS));
            log.warn("Password-reset day limit reached userId={} lockedUntil={}", user.getId(), limit.getLockedUntil());
        }
        rateLimitRepository.save(limit);
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
