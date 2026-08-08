package com.threadly.community.controller;

import com.threadly.community.dto.CommunityResponse;
import com.threadly.community.dto.CreateCommunityRequest;
import com.threadly.community.service.CommunityService;
import com.threadly.community.web.RequestIdentity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final RequestIdentity requestIdentity;

    @PostMapping
    public ResponseEntity<CommunityResponse> create(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "X-Username", required = false) String username,
            @Valid @RequestBody CreateCommunityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                communityService.create(requestIdentity.requireUserId(userId), username, request));
    }

    @GetMapping
    public ResponseEntity<Page<CommunityResponse>> list(
            @RequestParam(required = false) String q,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(communityService.list(q, userId, pageable));
    }

    @GetMapping("/discover")
    public ResponseEntity<Page<CommunityResponse>> discover(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PageableDefault(size = 12) Pageable pageable) {
        return ResponseEntity.ok(communityService.discover(userId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommunityResponse> getById(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(communityService.getById(id, userId));
    }

    @GetMapping("/by-slug/{slug}")
    public ResponseEntity<CommunityResponse> getBySlug(
            @PathVariable String slug,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(communityService.getBySlug(slug, userId));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<CommunityResponse> join(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "X-Username", required = false) String username) {
        return ResponseEntity.ok(communityService.join(id, requestIdentity.requireUserId(userId), username));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<CommunityResponse> leave(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(communityService.leave(id, requestIdentity.requireUserId(userId)));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<Page<UUID>> members(
            @PathVariable UUID id,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(communityService.memberIds(id, pageable));
    }

    @GetMapping("/memberships/user/{userId}")
    public ResponseEntity<List<UUID>> membershipsByUser(@PathVariable UUID userId) {
        // Keep full list for internal feed personalization (bounded by user's join count).
        return ResponseEntity.ok(communityService.joinedCommunityIds(userId));
    }

    @GetMapping("/memberships/user/{userId}/page")
    public ResponseEntity<Page<UUID>> membershipsByUserPage(
            @PathVariable UUID userId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(communityService.joinedCommunityIds(userId, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        communityService.delete(id, requestIdentity.requireUserId(userId));
        return ResponseEntity.noContent().build();
    }
}
