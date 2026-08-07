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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordResetTokenRepository tokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock MailService mailService;
    @Mock MailAppProperties mailProps;

    @InjectMocks PasswordResetServiceImpl service;

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
        User user = User.builder().id(5L).email("a@example.com").username("a").passwordHash("x").displayName("A").build();
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("a@example.com");
        when(userRepository.findActiveByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.forgotPassword(req);

        verify(tokenRepository).deleteUnusedForUser(5L);
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendPasswordReset(eq("a@example.com"), url.capture());
        assertTrue(url.getValue().contains("/reset-password?token="));
    }

    @Test
    void resetPasswordRejectsUnknownToken() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("bad");
        req.setNewPassword("newpass1");
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(anyString())).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.resetPassword(req));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void resetPasswordUpdatesHashWhenTokenValid() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("raw-token");
        req.setNewPassword("newpass1");

        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .userId(9L)
                .tokenHash("x")
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        User user = User.builder().id(9L).email("u@x.com").username("u")
                .passwordHash("old").displayName("U").build();

        when(tokenRepository.findByTokenHashAndUsedAtIsNull(anyString())).thenReturn(Optional.of(token));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass1")).thenReturn("hashed-new");

        MessageResponse res = service.resetPassword(req);

        assertTrue(res.getMessage().toLowerCase().contains("password updated"));
        assertEquals("hashed-new", user.getPasswordHash());
        assertNotNull(token.getUsedAt());
        verify(userRepository).save(user);
    }
}
