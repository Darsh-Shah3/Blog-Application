package com.threadly.auth.service.impl;

import com.threadly.auth.dto.LoginRequest;
import com.threadly.auth.dto.RegisterRequest;
import com.threadly.auth.entity.Role;
import com.threadly.auth.entity.User;
import com.threadly.auth.exception.ApiException;
import com.threadly.auth.mapper.UserMapper;
import com.threadly.auth.repository.RoleRepository;
import com.threadly.auth.repository.UserRepository;
import com.threadly.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock UserMapper userMapper;

    @InjectMocks AuthServiceImpl authService;

    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = Role.builder().id(1L).name("ROLE_USER").build();
    }

    @Test
    void registerPersistsUserWithUserRoleAndReturnsToken() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("darsh");
        req.setEmail("darsh@example.com");
        req.setPassword("secret12");
        req.setDisplayName("Darsh");

        when(userRepository.existsActiveByEmail("darsh@example.com")).thenReturn(false);
        when(userRepository.existsActiveByUsername("darsh")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("secret12")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(jwtTokenProvider.generateToken(eq(10L), eq("darsh"), anyList(), anyList()))
                .thenReturn("jwt-token");
        when(userMapper.toResponse(any(User.class))).thenReturn(
                com.threadly.auth.dto.UserResponse.builder()
                        .id(10L).username("darsh").roles(Set.of("ROLE_USER")).build());

        var res = authService.register(req);

        assertEquals("jwt-token", res.getAccessToken());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertTrue(captor.getValue().getRoles().stream().anyMatch(r -> "ROLE_USER".equals(r.getName())));
        assertEquals("hashed", captor.getValue().getPasswordHash());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("x");
        req.setEmail("taken@example.com");
        req.setPassword("secret12");
        when(userRepository.existsActiveByEmail("taken@example.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(req));
        assertEquals(409, ex.getStatus());
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginRejectsBadPassword() {
        User user = User.builder()
                .id(1L)
                .username("demo")
                .email("demo@example.com")
                .passwordHash("hash")
                .displayName("Demo")
                .build();
        user.getRoles().add(userRole);
        LoginRequest req = new LoginRequest();
        req.setEmailOrUsername("demo");
        req.setPassword("wrong");

        when(userRepository.findActiveByEmail("demo")).thenReturn(Optional.empty());
        when(userRepository.findActiveByUsername("demo")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(req));
        assertEquals(401, ex.getStatus());
        verify(jwtTokenProvider, never()).generateToken(anyLong(), anyString(), anyList(), anyList());
    }
}
