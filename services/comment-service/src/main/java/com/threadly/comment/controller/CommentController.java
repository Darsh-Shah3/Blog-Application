package com.threadly.comment.controller;

import com.threadly.comment.dto.CommentResponse;
import com.threadly.comment.dto.CreateCommentRequest;
import com.threadly.comment.dto.UpdateCommentRequest;
import com.threadly.comment.exception.ApiException;
import com.threadly.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "X-Username", required = false) String username,
            @Valid @RequestBody CreateCommentRequest request) {
        if (userId == null) {
            throw new ApiException("Authentication required", 401);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.create(userId, username, request));
    }

    /** Paginated top-level comments + nested reply trees (default 25 per page). */
    @GetMapping("/threads/{postId}")
    public ResponseEntity<Page<CommentResponse>> thread(
            @PathVariable UUID postId,
            @PageableDefault(size = 25) Pageable pageable) {
        return ResponseEntity.ok(commentService.pageRoots(postId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommentResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(commentService.get(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CommentResponse> update(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "X-Username", required = false) String username,
            @Valid @RequestBody UpdateCommentRequest request) {
        if (userId == null) {
            throw new ApiException("Authentication required", 401);
        }
        return ResponseEntity.ok(commentService.update(id, userId, username, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) {
            throw new ApiException("Authentication required", 401);
        }
        commentService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
