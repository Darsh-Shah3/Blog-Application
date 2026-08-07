package com.threadly.post.controller;

import com.threadly.post.dto.*;
import com.threadly.post.service.PostService;
import com.threadly.post.web.RequestIdentity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public post APIs. Score / comment-count mutations live under {@code /api/v1/internal/posts}.
 */
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final RequestIdentity requestIdentity;

    @PostMapping
    public ResponseEntity<PostResponse> create(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Username", required = false) String username,
            @Valid @RequestBody CreatePostRequest request) {
        Long uid = requestIdentity.requireUserId(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.create(uid, username, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(postService.get(id));
    }

    @GetMapping
    public ResponseEntity<Page<PostResponse>> feed(
            @RequestParam(defaultValue = "new") String sort,
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String q,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(postService.feed(sort, communityId, authorId, userId, q, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        postService.delete(id, requestIdentity.requireUserId(userId));
        return ResponseEntity.noContent().build();
    }
}
