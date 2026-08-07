package com.threadly.post.service.impl;

import com.threadly.post.dto.CreatePostRequest;
import com.threadly.post.dto.PostResponse;
import com.threadly.post.entity.Post;
import com.threadly.post.exception.ApiException;
import com.threadly.post.mapper.PostMapper;
import com.threadly.post.port.AuditPort;
import com.threadly.post.port.CommunityPort;
import com.threadly.post.port.CommunitySummary;
import com.threadly.post.port.NotificationPort;
import com.threadly.post.port.UserPort;
import com.threadly.post.port.UserSummary;
import com.threadly.post.repository.PostRepository;
import com.threadly.post.security.AccessControl;
import com.threadly.post.service.PostService;
import com.threadly.post.util.AuditActors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Post use-cases. Cross-service enrichment goes only through ports (not HTTP clients).
 */
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);

    private final PostRepository postRepository;
    private final CommunityPort communityPort;
    private final UserPort userPort;
    private final PostMapper postMapper;
    private final AuditPort auditPort;
    private final NotificationPort notificationPort;

    @Override
    @Transactional
    public PostResponse create(Long authorId, String actorUsername, CreatePostRequest request) {
        CommunitySummary community = communityPort.findById(request.getCommunityId())
                .orElseThrow(() -> new ApiException("Community not found", HttpStatus.BAD_REQUEST.value()));

        Post.PostType type = request.getPostType() == null ? Post.PostType.TEXT : request.getPostType();
        if (type == Post.PostType.LINK && (request.getLinkUrl() == null || request.getLinkUrl().isBlank())) {
            throw new ApiException("linkUrl required for LINK posts", HttpStatus.BAD_REQUEST.value());
        }
        if ((type == Post.PostType.IMAGE || type == Post.PostType.FILE) && request.getMediaId() == null) {
            throw new ApiException("mediaId required for IMAGE/FILE posts — upload via /api/v1/media/uploads first",
                    HttpStatus.BAD_REQUEST.value());
        }

        UserSummary author = userPort.findById(authorId).orElse(null);
        String by = author != null && author.username() != null
                ? author.username()
                : AuditActors.resolve(actorUsername, authorId);

        Post post = Post.builder()
                .communityId(request.getCommunityId())
                .authorId(authorId)
                .title(request.getTitle().trim())
                .content(request.getContent())
                .postType(type)
                .linkUrl(request.getLinkUrl())
                .mediaId(request.getMediaId())
                .score(0L)
                .commentCount(0L)
                .createdBy(by)
                .updatedBy(by)
                .build();
        post = postRepository.save(post);
        log.info("Post created id={} type={} mediaId={} communityId={} authorId={} createdBy={}",
                post.getId(), type, post.getMediaId(), post.getCommunityId(), authorId, by);
        auditPort.record("CREATE", "POST", String.valueOf(post.getId()), authorId, by,
                "Post created: " + post.getTitle());
        notificationPort.notifyPostCreated(authorId, by, post.getId(), post.getTitle(), community.slug());
        return postMapper.toResponse(post, community, author);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse get(Long id) {
        return enrich(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> feed(String sort, Long communityId, Long authorId, Long userId, String q, Pageable pageable) {
        Pageable sorted = applySort(sort, pageable);
        Page<Post> page;
        if (q != null && !q.isBlank()) {
            page = postRepository.searchByTitle(q.trim(), sorted);
        } else if (communityId != null) {
            page = postRepository.findByCommunityId(communityId, sorted);
        } else if (authorId != null) {
            page = postRepository.findByAuthorId(authorId, sorted);
        } else if (userId != null) {
            // Personalized home: only posts from communities the viewer joined.
            List<Long> joined = communityPort.findJoinedCommunityIds(userId);
            page = joined.isEmpty()
                    ? postRepository.findAll(sorted)
                    : postRepository.findByCommunityIdIn(joined, sorted);
        } else {
            page = postRepository.findAll(sorted);
        }
        return page.map(this::enrich);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        Post post = find(id);
        boolean owner = post.getAuthorId().equals(userId);
        if (!owner && !AccessControl.canModerateContent()) {
            throw new ApiException("Not allowed to delete this post", HttpStatus.FORBIDDEN.value());
        }
        postRepository.delete(post);
        auditPort.record("DELETE", "POST", String.valueOf(id), userId, null,
                "Post deleted id=" + id);
        log.info("Post deleted id={} by userId={} moderated={}", id, userId, !owner);
    }

    @Override
    @Transactional
    public PostResponse adjustScore(Long id, long delta) {
        Post post = find(id);
        post.setScore(post.getScore() + delta);
        post.setUpdatedBy("system");
        return enrich(postRepository.save(post));
    }

    @Override
    @Transactional
    public PostResponse adjustCommentCount(Long id, long delta) {
        Post post = find(id);
        post.setCommentCount(Math.max(0, post.getCommentCount() + delta));
        post.setUpdatedBy("system");
        return enrich(postRepository.save(post));
    }

    private Post find(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ApiException("Post not found", HttpStatus.NOT_FOUND.value()));
    }

    /**
     * Maps UI sort keys (hot/new/top) to multi-column database ordering.
     * "hot" ≈ score + engagement; true Reddit hot needs time-decay (future).
     */
    private Pageable applySort(String sort, Pageable pageable) {
        String s = sort == null ? "new" : sort.toLowerCase();
        return switch (s) {
            case "top" -> PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "score").and(Sort.by(Sort.Direction.DESC, "createdAt")));
            case "hot" -> PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "score")
                            .and(Sort.by(Sort.Direction.DESC, "commentCount"))
                            .and(Sort.by(Sort.Direction.DESC, "createdAt")));
            default -> PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
        };
    }

    private PostResponse enrich(Post post) {
        CommunitySummary community = communityPort.findById(post.getCommunityId()).orElse(null);
        UserSummary author = userPort.findById(post.getAuthorId()).orElse(null);
        return postMapper.toResponse(post, community, author);
    }
}
