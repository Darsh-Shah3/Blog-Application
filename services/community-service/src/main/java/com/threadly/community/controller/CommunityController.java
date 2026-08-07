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

@RestController
@RequestMapping("/api/v1/communities")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final RequestIdentity requestIdentity;

    @PostMapping
    public ResponseEntity<CommunityResponse> create(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Username", required = false) String username,
            @Valid @RequestBody CreateCommunityRequest request) {
        Long uid = requestIdentity.requireUserId(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(communityService.create(uid, username, request));
    }

    @GetMapping
    public ResponseEntity<Page<CommunityResponse>> list(
            @RequestParam(required = false) String q,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(communityService.list(q, userId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommunityResponse> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(communityService.getById(id, userId));
    }

    @GetMapping("/by-slug/{slug}")
    public ResponseEntity<CommunityResponse> getBySlug(
            @PathVariable String slug,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(communityService.getBySlug(slug, userId));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<CommunityResponse> join(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(communityService.join(id, requestIdentity.requireUserId(userId)));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<CommunityResponse> leave(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(communityService.leave(id, requestIdentity.requireUserId(userId)));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<Long>> members(@PathVariable Long id) {
        return ResponseEntity.ok(communityService.memberIds(id));
    }

    /** Community IDs the user has joined (used for personalized home feed). */
    @GetMapping("/memberships/user/{userId}")
    public ResponseEntity<List<Long>> membershipsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(communityService.joinedCommunityIds(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        communityService.delete(id, requestIdentity.requireUserId(userId));
        return ResponseEntity.noContent().build();
    }
}
