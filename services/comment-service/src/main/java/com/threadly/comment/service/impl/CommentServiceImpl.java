package com.threadly.comment.service.impl;

import com.threadly.comment.dto.CommentResponse;
import com.threadly.comment.dto.CreateCommentRequest;
import com.threadly.comment.entity.Comment;
import com.threadly.comment.exception.ApiException;
import com.threadly.comment.port.PostPort;
import com.threadly.comment.port.UserPort;
import com.threadly.comment.port.UserSummary;
import com.threadly.comment.repository.CommentRepository;
import com.threadly.comment.security.AccessControl;
import com.threadly.comment.service.CommentService;
import com.threadly.comment.util.AuditActors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);

    private final CommentRepository commentRepository;
    private final PostPort postPort;
    private final UserPort userPort;

    /**
     * Saves the comment, then bumps post.commentCount remotely.
     * Remote failure throws → Spring rolls back this local transaction (no orphan comment row).
     */
    @Override
    @Transactional
    public CommentResponse create(Long authorId, String actorUsername, CreateCommentRequest request) {
        postPort.ensureExists(request.getPostId());
        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ApiException("Parent comment not found", HttpStatus.BAD_REQUEST.value()));
            if (!parent.getPostId().equals(request.getPostId())) {
                throw new ApiException("Parent comment belongs to another post", HttpStatus.BAD_REQUEST.value());
            }
        }
        UserSummary author = userPort.findById(authorId).orElse(null);
        String by = author != null && author.username() != null
                ? author.username()
                : AuditActors.resolve(actorUsername, authorId);
        Comment comment = Comment.builder()
                .postId(request.getPostId())
                .authorId(authorId)
                .parentId(request.getParentId())
                .content(request.getContent().trim())
                .score(0L)
                .createdBy(by)
                .updatedBy(by)
                .build();
        comment = commentRepository.save(comment);
        postPort.adjustCommentCount(request.getPostId(), 1);
        log.info("Comment created id={} postId={} authorId={} createdBy={}",
                comment.getId(), comment.getPostId(), authorId, by);
        return toResponse(comment, new ArrayList<>());
    }

    /**
     * Builds a nested tree in O(n) via two passes:
     * 1) index all nodes, 2) attach each node under its parent (or root).
     * Avoids recursive DB queries so deep Reddit-style threads stay cheap.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> treeForPost(Long postId) {
        List<Comment> all = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        Map<Long, CommentResponse> map = new HashMap<>();
        List<CommentResponse> roots = new ArrayList<>();
        for (Comment c : all) {
            map.put(c.getId(), toResponse(c, new ArrayList<>()));
        }
        for (Comment c : all) {
            CommentResponse node = map.get(c.getId());
            if (c.getParentId() == null) {
                roots.add(node);
            } else {
                CommentResponse parent = map.get(c.getParentId());
                if (parent != null) {
                    parent.getReplies().add(node);
                } else {
                    // Orphaned parent reference — promote to root rather than losing the comment.
                    roots.add(node);
                }
            }
        }
        return roots;
    }

    /**
     * Deletes locally first, then decrements post.commentCount.
     * If remote count update fails, throw so local delete rolls back (comment restored).
     */
    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Comment not found", HttpStatus.NOT_FOUND.value()));
        boolean owner = comment.getAuthorId().equals(userId);
        if (!owner && !AccessControl.canModerateContent()) {
            throw new ApiException("Not allowed", HttpStatus.FORBIDDEN.value());
        }
        Long postId = comment.getPostId();
        commentRepository.delete(comment);
        postPort.adjustCommentCount(postId, -1);
        log.info("Comment deleted id={} by userId={} moderated={}", id, userId, !owner);
    }

    @Override
    @Transactional
    public CommentResponse adjustScore(Long id, long delta) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Comment not found", HttpStatus.NOT_FOUND.value()));
        comment.setScore(comment.getScore() + delta);
        comment.setUpdatedBy("system");
        return toResponse(commentRepository.save(comment), new ArrayList<>());
    }

    @Override
    @Transactional(readOnly = true)
    public CommentResponse get(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Comment not found", HttpStatus.NOT_FOUND.value()));
        return toResponse(comment, new ArrayList<>());
    }

    private CommentResponse toResponse(Comment c, List<CommentResponse> replies) {
        UserSummary author = userPort.findById(c.getAuthorId())
                .orElse(new UserSummary(c.getAuthorId(), "user-" + c.getAuthorId()));
        return CommentResponse.builder()
                .id(c.getId())
                .postId(c.getPostId())
                .authorId(c.getAuthorId())
                .authorUsername(author.username())
                .parentId(c.getParentId())
                .content(c.getContent())
                .score(c.getScore())
                .createdAt(c.getCreatedAt())
                .createdBy(c.getCreatedBy())
                .updatedBy(c.getUpdatedBy())
                .replies(replies)
                .build();
    }
}
