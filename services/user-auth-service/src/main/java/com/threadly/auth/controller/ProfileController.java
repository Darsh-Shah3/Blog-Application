package com.threadly.auth.controller;

import com.threadly.auth.dto.UserResponse;
import com.threadly.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final AuthService authService;

    @GetMapping("/{username}")
    public ResponseEntity<UserResponse> byUsername(@PathVariable String username) {
        return ResponseEntity.ok(publicCard(authService.getByUsername(username)));
    }

    @GetMapping("/by-id/{id}")
    public ResponseEntity<UserResponse> byId(@PathVariable UUID id) {
        return ResponseEntity.ok(publicCard(authService.getById(id)));
    }

    /** Public cards never expose email. */
    private static UserResponse publicCard(UserResponse user) {
        if (user != null) {
            user.setEmail(null);
        }
        return user;
    }
}
