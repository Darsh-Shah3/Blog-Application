package com.threadly.auth.controller;

import com.threadly.auth.dto.FollowStatusResponse;
import com.threadly.auth.dto.UserResponse;
import com.threadly.auth.exception.ApiException;
import com.threadly.auth.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{username}/follow")
    public ResponseEntity<FollowStatusResponse> follow(
            @PathVariable String username,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(followService.follow(requireUser(userId), username));
    }

    @DeleteMapping("/{username}/follow")
    public ResponseEntity<FollowStatusResponse> unfollow(
            @PathVariable String username,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(followService.unfollow(requireUser(userId), username));
    }

    @GetMapping("/{username}/follow-status")
    public ResponseEntity<FollowStatusResponse> status(
            @PathVariable String username,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(followService.status(userId, username));
    }

    @GetMapping("/{username}/followers")
    public ResponseEntity<List<UserResponse>> followers(@PathVariable String username) {
        return ResponseEntity.ok(followService.followers(username));
    }

    @GetMapping("/{username}/following")
    public ResponseEntity<List<UserResponse>> following(@PathVariable String username) {
        return ResponseEntity.ok(followService.following(username));
    }

    private Long requireUser(Long userId) {
        if (userId == null) {
            throw new ApiException("Authentication required", 401);
        }
        return userId;
    }
}
