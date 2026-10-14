package com.threadly.comment.service.impl;

import com.threadly.comment.dto.CommentResponse;
import com.threadly.comment.dto.CreateCommentRequest;
import com.threadly.comment.dto.UpdateCommentRequest;
import com.threadly.comment.entity.Comment;
import com.threadly.comment.exception.ApiException;
import com.threadly.comment.port.PostPort;
import com.threadly.comment.port.PostSummary;
import com.threadly.comment.port.UserPort;
import com.threadly.comment.port.UserSummary;
import com.threadly.comment.repository.CommentRepository;
import com.threadly.comment.security.AccessControl;
import com.threadly.comment.service.CommentService;
import com.threadly.comment.util.AuditActors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);

    private final CommentRepository commentRepository;
    private final PostPort postPort;
    private final UserPort userPort;

    @Override
    @Transactional
    public CommentResponse create(UUID authorId, String actorUsername, CreateCommentRequest request) {
        PostSummary post = postPort.findById(request.getPostId())
                .orElseThrow(() -> new ApiException("Post not found", HttpStatus.BAD_REQUEST.value()));

        Map<String, Object> parentSnap = null;
        UUID parentId = request.getParentId();
        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new ApiException("Parent comment not found", HttpStatus.BAD_REQUEST.value()));
            if (!parent.getPostId().equals(request.getPostId())) {
                throw new ApiException("Parent comment belongs to another post", HttpStatus.BAD_REQUEST.value());
            }
            parentSnap = Map.of(
                    "id", parent.getId().toString(),
                    "authorUsername", parent.getAuthor() != null && parent.getAuthor().get("username") != null
                            ? parent.getAuthor().get("username") : "",
                    "contentPreview", preview(parent.getContent())
            );
        }

        UserSummary author = userPort.findById(authorId).orElse(null);
        String by = author != null && author.username() != null
                ? author.username()
                : AuditActors.resolve(actorUsername, authorId);

        Comment comment = Comment.builder()
                .postId(post.id())
                .post(Map.of("id", post.id().toString(), "title", post.title() == null ? "" : post.title()))
                .authorId(authorId)
                .author(Map.of("id", authorId.toString(), "username", by))
                .parentId(parentId)
                .parent(parentSnap)
                .content(request.getContent().trim())
                .score(0L)
                .createdBy(by)
                .updatedBy(by)
                .build();
        comment = commentRepository.save(comment);
        postPort.adjustCommentCount(request.getPostId(), 1);
        log.info("Comment created id={} postId={} authorId={}", comment.getId(), comment.getPostId(), authorId);
        return toResponse(comment, new ArrayList<>());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> pageRoots(UUID postId, Pageable pageable) {
        Pageable capped = pageable.getPageSize() > 50
                ? org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), 50, pageable.getSort())
                : pageable;
        Page<Comment> roots = commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtDesc(postId, capped);
        if (roots.isEmpty()) {
            return new PageImpl<>(List.of(), capped, 0);
        }

        List<UUID> rootIds = roots.getContent().stream().map(Comment::getId).toList();
        // Load only the subtree under this page of roots (not every comment on the post).
        List<Comment> subtree = new ArrayList<>();
        List<UUID> frontier = rootIds;
        int safety = 0;
        while (!frontier.isEmpty() && safety++ < 20) {
            List<Comment> children = commentRepository.findByPostIdAndParentIdInOrderByCreatedAtAsc(postId, frontier);
            if (children.isEmpty()) {
                break;
            }
            subtree.addAll(children);
            frontier = children.stream().map(Comment::getId).toList();
            if (subtree.size() > 5000) {
                break;
            }
        }

        Map<UUID, CommentResponse> map = new HashMap<>();
        for (Comment c : roots.getContent()) {
            map.put(c.getId(), toResponse(c, new ArrayList<>()));
        }
        for (Comment c : subtree) {
            map.put(c.getId(), toResponse(c, new ArrayList<>()));
        }
        for (Comment c : subtree) {
            CommentResponse node = map.get(c.getId());
            CommentResponse parent = map.get(c.getParentId());
            if (parent != null && node != null) {
                parent.getReplies().add(node);
            }
        }

        List<CommentResponse> ordered = rootIds.stream()
                .map(map::get)
                .filter(r -> r != null)
                .toList();

        return new PageImpl<>(ordered, capped, roots.getTotalElements());
    }

    @Override
    @Transactional
    public CommentResponse update(UUID id, UUID userId, String actorUsername, UpdateCommentRequest request) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Comment not found", HttpStatus.NOT_FOUND.value()));
        if (!comment.getAuthorId().equals(userId)) {
            throw new ApiException("Only the author can edit this comment", HttpStatus.FORBIDDEN.value());
        }
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isEmpty()) {
            throw new ApiException("Comment cannot be empty", HttpStatus.BAD_REQUEST.value());
        }
        String by = AuditActors.resolve(actorUsername, userId);
        comment.setContent(content);
        comment.setUpdatedBy(by);
        Comment saved = commentRepository.save(comment);
        log.info("Comment updated id={} by userId={}", id, userId);
        return toResponse(saved, new ArrayList<>());
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID userId) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Comment not found", HttpStatus.NOT_FOUND.value()));
        boolean owner = comment.getAuthorId().equals(userId);
        if (!owner && !AccessControl.canModerateContent()) {
            throw new ApiException("Not allowed", HttpStatus.FORBIDDEN.value());
        }
        UUID postId = comment.getPostId();
        commentRepository.delete(comment);
        postPort.adjustCommentCount(postId, -1);
        log.info("Comment deleted id={} by userId={}", id, userId);
    }

    @Override
    @Transactional
    public CommentResponse adjustScore(UUID id, long delta) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Comment not found", HttpStatus.NOT_FOUND.value()));
        comment.setScore(comment.getScore() + delta);
        comment.setUpdatedBy("system");
        return toResponse(commentRepository.save(comment), new ArrayList<>());
    }

    @Override
    @Transactional(readOnly = true)
    public CommentResponse get(UUID id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Comment not found", HttpStatus.NOT_FOUND.value()));
        return toResponse(comment, new ArrayList<>());
    }

    private CommentResponse toResponse(Comment c, List<CommentResponse> replies) {
        String username = c.getAuthor() != null && c.getAuthor().get("username") != null
                ? String.valueOf(c.getAuthor().get("username"))
                : "user";
        return CommentResponse.builder()
                .id(c.getId())
                .postId(c.getPostId())
                .post(c.getPost())
                .authorId(c.getAuthorId())
                .authorUsername(username)
                .author(c.getAuthor())
                .parentId(c.getParentId())
                .parent(c.getParent())
                .content(c.getContent())
                .score(c.getScore())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .createdBy(c.getCreatedBy())
                .updatedBy(c.getUpdatedBy())
                .replies(replies)
                .build();
    }

    private static String preview(String content) {
        if (content == null) return "";
        return content.length() <= 80 ? content : content.substring(0, 80) + "…";
    }
}
