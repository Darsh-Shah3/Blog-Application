package com.threadly.post.controller;

import com.threadly.post.dto.CreatePostRequest;
import com.threadly.post.dto.PostResponse;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final RequestIdentity requestIdentity;

    @PostMapping
    public ResponseEntity<PostResponse> create(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "X-Username", required = false) String username,
            @Valid @RequestBody CreatePostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                postService.create(requestIdentity.requireUserId(userId), username, request));
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<List<PostResponse>> related(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(postService.related(id, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(postService.get(id));
    }

    @GetMapping
    public ResponseEntity<Page<PostResponse>> feed(
            @RequestParam(defaultValue = "for_you") String sort,
            @RequestParam(required = false) UUID communityId,
            @RequestParam(required = false) UUID authorId,
            @RequestParam(required = false) String q,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(postService.feed(sort, communityId, authorId, userId, q, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        postService.delete(id, requestIdentity.requireUserId(userId));
        return ResponseEntity.noContent().build();
    }
}
