package com.threadly.auth.controller;

import com.threadly.auth.dto.FollowerContactResponse;
import com.threadly.auth.dto.KarmaDeltaRequest;
import com.threadly.auth.dto.UserResponse;
import com.threadly.auth.repository.UserRepository;
import com.threadly.auth.service.AuthService;
import com.threadly.auth.service.FollowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Service-to-service user APIs (internal API key only).
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('INTERNAL')")
public class InternalUserController {

    private final AuthService authService;
    private final FollowService followService;
    private final UserRepository userRepository;

    @PostMapping("/api/v1/internal/users/{id}/karma-delta")
    public ResponseEntity<UserResponse> applyKarmaDelta(
            @PathVariable Long id,
            @Valid @RequestBody KarmaDeltaRequest request) {
        return ResponseEntity.ok(authService.adjustKarma(id, request.getDelta()));
    }

    @GetMapping("/api/v1/internal/users/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(authService.getById(id));
    }

    /** Active followers with email — used by notification fan-out. */
    @GetMapping("/api/v1/internal/users/{id}/followers")
    public ResponseEntity<List<FollowerContactResponse>> followers(@PathVariable Long id) {
        return ResponseEntity.ok(followService.internalFollowers(id));
    }

    @GetMapping("/api/v1/internal/stats/users")
    public ResponseEntity<Map<String, Long>> userStats() {
        return ResponseEntity.ok(Map.of("count", userRepository.countActive()));
    }
}
