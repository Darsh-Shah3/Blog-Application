package com.threadly.auth.service.impl;

import com.threadly.auth.config.MailAppProperties;
import com.threadly.auth.dto.ForgotPasswordRequest;
import com.threadly.auth.dto.MessageResponse;
import com.threadly.auth.dto.ResetPasswordRequest;
import com.threadly.auth.entity.PasswordResetToken;
import com.threadly.auth.entity.User;
import com.threadly.auth.exception.ApiException;
import com.threadly.auth.repository.PasswordResetRateLimitRepository;
import com.threadly.auth.repository.PasswordResetTokenRepository;
import com.threadly.auth.repository.UserRepository;
import com.threadly.auth.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordResetTokenRepository tokenRepository;
    @Mock PasswordResetRateLimitRepository rateLimitRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock MailService mailService;
    @Mock MailAppProperties mailProps;

    @InjectMocks PasswordResetServiceImpl service;

    private final UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void mailDefaults() {
        lenient().when(mailProps.getResetTokenMinutes()).thenReturn(30L);
        lenient().when(mailProps.getFrontendBaseUrl()).thenReturn("http://localhost:3000");
    }

    @Test
    void forgotPasswordUnknownEmailStillReturnsGenericSuccess() {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("nobody@example.com");
        when(userRepository.findActiveByEmail("nobody@example.com")).thenReturn(Optional.empty());

        MessageResponse res = service.forgotPassword(req);

        assertTrue(res.getMessage().toLowerCase().contains("if that email"));
        verify(tokenRepository, never()).save(any());
        verify(mailService, never()).sendPasswordReset(anyString(), anyString());
    }

    @Test
    void forgotPasswordKnownEmailCreatesTokenAndMails() {
        User user = User.builder().id(userId).email("a@example.com").username("a")
                .passwordHash("x").displayName("A").build();
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("a@example.com");
        when(userRepository.findActiveByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(rateLimitRepository.findById(userId)).thenReturn(Optional.empty());
        when(rateLimitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.forgotPassword(req);

        verify(tokenRepository).deleteUnusedForUser(userId);
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendPasswordReset(eq("a@example.com"), url.capture());
        assertTrue(url.getValue().contains("/reset-password?token="));
    }

    @Test
    void resetPasswordRejectsUnknownToken() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("bad");
        req.setNewPassword("newpass1");
        when(tokenRepository.findByTokenHashAndUsedFalse(anyString())).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.resetPassword(req));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void resetPasswordUpdatesHashWhenTokenValid() {
        UUID uid = UUID.fromString("22222222-2222-2222-2222-222222222222");
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("raw-token");
        req.setNewPassword("newpass1");

        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(uid)
                .username("u")
                .tokenHash("x")
                .expiresAt(Instant.now().plusSeconds(600))
                .used(false)
                .build();
        User user = User.builder().id(uid).email("u@x.com").username("u")
                .passwordHash("old").displayName("U").build();

        when(tokenRepository.findByTokenHashAndUsedFalse(anyString())).thenReturn(Optional.of(token));
        when(userRepository.findById(uid)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass1")).thenReturn("hashed-new");

        MessageResponse res = service.resetPassword(req);

        assertTrue(res.getMessage().toLowerCase().contains("password updated"));
        assertEquals("hashed-new", user.getPasswordHash());
        assertTrue(token.isUsed());
        assertNotNull(token.getUsedAt());
        verify(userRepository).save(user);
    }
}
