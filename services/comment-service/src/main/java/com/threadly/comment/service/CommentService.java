package com.threadly.comment.service;

import com.threadly.comment.dto.CommentResponse;
import com.threadly.comment.dto.CreateCommentRequest;
import com.threadly.comment.dto.UpdateCommentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CommentService {

    CommentResponse create(UUID authorId, String actorUsername, CreateCommentRequest request);

    /**
     * Page of top-level comments with nested replies attached (YouTube-style).
     */
    Page<CommentResponse> pageRoots(UUID postId, Pageable pageable);

    void delete(UUID id, UUID userId);

    CommentResponse update(UUID id, UUID userId, String actorUsername, UpdateCommentRequest request);

    CommentResponse adjustScore(UUID id, long delta);

    CommentResponse get(UUID id);
}
