package com.threadly.comment.repository;

import com.threadly.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByPostIdOrderByCreatedAtAsc(UUID postId);

    /** Top-level comments (YouTube-style page), oldest first within the page batch from repo; service can re-sort. */
    Page<Comment> findByPostIdAndParentIdIsNullOrderByCreatedAtDesc(UUID postId, Pageable pageable);

    List<Comment> findByPostIdAndParentIdInOrderByCreatedAtAsc(UUID postId, List<UUID> parentIds);

    @Query("select count(c) from Comment c where c.postId = :postId and c.parentId is null")
    long countRootsByPostId(@Param("postId") UUID postId);
}
