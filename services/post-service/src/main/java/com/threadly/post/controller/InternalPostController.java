package com.threadly.post.controller;

import com.threadly.post.dto.CommentCountDeltaRequest;
import com.threadly.post.dto.PostResponse;
import com.threadly.post.dto.ScoreDeltaRequest;
import com.threadly.post.repository.PostRepository;
import com.threadly.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Cross-service post mutations (score / comment count). Not exposed via the public gateway.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('INTERNAL')")
public class InternalPostController {

    private final PostService postService;
    private final PostRepository postRepository;

    @PostMapping("/api/v1/internal/posts/{id}/score-delta")
    public ResponseEntity<PostResponse> applyScoreDelta(
            @PathVariable Long id,
            @Valid @RequestBody ScoreDeltaRequest request) {
        return ResponseEntity.ok(postService.adjustScore(id, request.getDelta()));
    }

    @PostMapping("/api/v1/internal/posts/{id}/comment-count-delta")
    public ResponseEntity<PostResponse> applyCommentCountDelta(
            @PathVariable Long id,
            @Valid @RequestBody CommentCountDeltaRequest request) {
        return ResponseEntity.ok(postService.adjustCommentCount(id, request.getDelta()));
    }

    @GetMapping("/api/v1/internal/stats/posts")
    public ResponseEntity<Map<String, Long>> postStats() {
        return ResponseEntity.ok(Map.of("count", postRepository.count()));
    }
}
