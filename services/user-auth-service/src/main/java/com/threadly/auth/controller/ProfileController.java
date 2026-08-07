package com.threadly.auth.controller;

import com.threadly.auth.dto.UserResponse;
import com.threadly.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public user profiles (username-first identity for Reddit-style URLs).
 */
@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final AuthService authService;

    @GetMapping("/{username}")
    public ResponseEntity<UserResponse> byUsername(@PathVariable String username) {
        return ResponseEntity.ok(authService.getByUsername(username));
    }

    @GetMapping("/by-id/{id}")
    public ResponseEntity<UserResponse> byId(@PathVariable Long id) {
        return ResponseEntity.ok(authService.getById(id));
    }
}
