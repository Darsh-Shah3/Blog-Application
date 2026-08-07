package com.threadly.comment.controller;

import com.threadly.comment.dto.CommentResponse;
import com.threadly.comment.dto.ScoreDeltaRequest;
import com.threadly.comment.repository.CommentRepository;
import com.threadly.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('INTERNAL')")
public class InternalCommentController {

    private final CommentService commentService;
    private final CommentRepository commentRepository;

    @PostMapping("/api/v1/internal/comments/{id}/score-delta")
    public ResponseEntity<CommentResponse> applyScoreDelta(
            @PathVariable Long id,
            @Valid @RequestBody ScoreDeltaRequest request) {
        return ResponseEntity.ok(commentService.adjustScore(id, request.getDelta()));
    }

    @GetMapping("/api/v1/internal/stats/comments")
    public ResponseEntity<Map<String, Long>> commentStats() {
        return ResponseEntity.ok(Map.of("count", commentRepository.count()));
    }
}
