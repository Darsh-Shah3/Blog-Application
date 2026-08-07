package com.threadly.comment.service;

import com.threadly.comment.dto.CommentResponse;
import com.threadly.comment.dto.CreateCommentRequest;

import java.util.List;

public interface CommentService {

    CommentResponse create(Long authorId, String actorUsername, CreateCommentRequest request);

    List<CommentResponse> treeForPost(Long postId);

    void delete(Long id, Long userId);

    CommentResponse adjustScore(Long id, long delta);

    CommentResponse get(Long id);
}
