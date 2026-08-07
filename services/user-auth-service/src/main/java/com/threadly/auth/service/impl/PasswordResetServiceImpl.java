package com.threadly.auth.service.impl;

import com.threadly.auth.config.MailAppProperties;
import com.threadly.auth.dto.ForgotPasswordRequest;
import com.threadly.auth.dto.MessageResponse;
import com.threadly.auth.dto.ResetPasswordRequest;
import com.threadly.auth.entity.PasswordResetToken;
import com.threadly.auth.entity.User;
import com.threadly.auth.exception.ApiException;
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
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final MailAppProperties mailProps;

    /**
     * Always returns the same generic message to avoid email enumeration.
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
        tokenRepository.deleteUnusedForUser(user.getId());

        String rawToken = generateRawToken();
        Instant expires = Instant.now().plusSeconds(mailProps.getResetTokenMinutes() * 60);

        tokenRepository.save(PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(sha256(rawToken))
                .expiresAt(expires)
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

        log.info("Password reset issued userId={} expiresAt={}", user.getId(), expires);
        return MessageResponse.builder().message(generic).build();
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String hash = sha256(request.getToken().trim());
        PasswordResetToken token = tokenRepository.findByTokenHashAndUsedAtIsNull(hash)
                .orElseThrow(() -> new ApiException(
                        "Invalid or expired reset link. Request a new one.",
                        HttpStatus.BAD_REQUEST.value()));

        if (token.getExpiresAt().isBefore(Instant.now())) {
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

        token.setUsedAt(Instant.now());
        tokenRepository.save(token);
        tokenRepository.deleteUnusedForUser(user.getId());

        log.info("Password reset completed userId={}", user.getId());
        return MessageResponse.builder().message("Password updated. You can sign in now.").build();
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
