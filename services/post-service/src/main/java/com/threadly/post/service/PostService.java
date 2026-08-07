package com.threadly.post.service;

import com.threadly.post.dto.CreatePostRequest;
import com.threadly.post.dto.PostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostService {

    PostResponse create(Long authorId, String actorUsername, CreatePostRequest request);

    PostResponse get(Long id);

    Page<PostResponse> feed(String sort, Long communityId, Long authorId, Long userId, String q, Pageable pageable);

    void delete(Long id, Long userId);

    PostResponse adjustScore(Long id, long delta);

    PostResponse adjustCommentCount(Long id, long delta);
}
