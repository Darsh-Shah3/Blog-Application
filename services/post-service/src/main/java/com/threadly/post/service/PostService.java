package com.threadly.post.service;

import com.threadly.post.dto.CreatePostRequest;
import com.threadly.post.dto.PostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PostService {

    PostResponse create(UUID authorId, String actorUsername, CreatePostRequest request);

    PostResponse get(UUID id);

    Page<PostResponse> feed(String sort, UUID communityId, UUID authorId, UUID userId, String q, Pageable pageable);

    List<PostResponse> related(UUID postId, int limit);

    void delete(UUID id, UUID userId);

    PostResponse adjustScore(UUID id, long delta);

    PostResponse adjustCommentCount(UUID id, long delta);
}
