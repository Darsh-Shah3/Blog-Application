package com.threadly.comment.controller;

import com.threadly.comment.dto.CommentResponse;
import com.threadly.comment.dto.CreateCommentRequest;
import com.threadly.comment.exception.ApiException;
import com.threadly.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Nested discussion APIs. Thread listing is namespaced under {@code /threads/{postId}}.
 */
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Username", required = false) String username,
            @Valid @RequestBody CreateCommentRequest request) {
        if (userId == null) {
            throw new ApiException("Authentication required", 401);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.create(userId, username, request));
    }

    /** Full nested comment tree for one post. */
    @GetMapping("/threads/{postId}")
    public ResponseEntity<List<CommentResponse>> thread(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.treeForPost(postId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommentResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(commentService.get(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            throw new ApiException("Authentication required", 401);
        }
        commentService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
