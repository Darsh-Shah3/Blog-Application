package com.threadly.vote.controller;

import com.threadly.vote.dto.CastVoteRequest;
import com.threadly.vote.dto.VoteResponse;
import com.threadly.vote.entity.Vote;
import com.threadly.vote.exception.ApiException;
import com.threadly.vote.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @PostMapping
    public ResponseEntity<VoteResponse> cast(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "X-Username", required = false) String username,
            @Valid @RequestBody CastVoteRequest request) {
        if (userId == null) {
            throw new ApiException("Authentication required", 401);
        }
        return ResponseEntity.ok(voteService.cast(userId, username, request));
    }

    @GetMapping("/current")
    public ResponseEntity<VoteResponse> currentVote(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestParam Vote.TargetType targetType,
            @RequestParam UUID targetId) {
        if (userId == null) {
            throw new ApiException("Authentication required", 401);
        }
        return ResponseEntity.ok(voteService.myVote(userId, targetType, targetId));
    }
}
