package com.threadly.post.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadly.post.cache.CachedPage;
import com.threadly.post.cache.RedisJsonCache;
import com.threadly.post.dto.CreatePostRequest;
import com.threadly.post.dto.PostResponse;
import com.threadly.post.entity.Post;
import com.threadly.post.exception.ApiException;
import com.threadly.post.mapper.PostMapper;
import com.threadly.post.model.MediaRef;
import com.threadly.post.model.NamedRef;
import com.threadly.post.model.UserRef;
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
import com.threadly.post.util.PgUuidArray;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);
    private static final int MAX_PAGE_SIZE = 50;
    private static final int TRENDING_HOURS = 48;
    private static final Duration DETAIL_TTL = Duration.ofSeconds(90);

    private final PostRepository postRepository;
    private final CommunityPort communityPort;
    private final UserPort userPort;
    private final PostMapper postMapper;
    private final AuditPort auditPort;
    private final NotificationPort notificationPort;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<RedisJsonCache> cacheProvider;

    @Value("${app.cache.redis-enabled:false}")
    private boolean cacheEnabled;

    @Override
    @Transactional
    public PostResponse create(UUID authorId, String actorUsername, CreatePostRequest request) {
        CommunitySummary community = communityPort.findById(request.getCommunityId())
                .orElseThrow(() -> new ApiException("Community not found", HttpStatus.BAD_REQUEST.value()));

        Post.PostType type = request.getPostType() == null ? Post.PostType.TEXT : request.getPostType();
        List<String> links = request.getLinkUrls() == null ? List.of() : request.getLinkUrls().stream()
                .filter(u -> u != null && !u.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(ArrayList::new));
        List<MediaRef> media = request.getMedia() == null ? List.of() : new ArrayList<>(request.getMedia());

        if (type == Post.PostType.LINK && links.isEmpty()) {
            throw new ApiException("At least one linkUrl is required for LINK posts", HttpStatus.BAD_REQUEST.value());
        }
        if ((type == Post.PostType.IMAGE || type == Post.PostType.FILE) && media.isEmpty()) {
            throw new ApiException("At least one media item is required for IMAGE/FILE posts",
                    HttpStatus.BAD_REQUEST.value());
        }

        UserSummary author = userPort.findById(authorId).orElse(null);
        String by = author != null && author.username() != null
                ? author.username()
                : AuditActors.resolve(actorUsername, authorId);

        Instant when = Instant.now();
        Post.Status status = Post.Status.PUBLISHED;
        Instant scheduledAt = request.getScheduledAt();
        if (scheduledAt != null && scheduledAt.isAfter(Instant.now().plusSeconds(30))) {
            status = Post.Status.SCHEDULED;
            when = scheduledAt;
        } else {
            scheduledAt = null;
        }

        Post post = Post.builder()
                .communityId(community.id())
                .community(NamedRef.builder().id(community.id()).name(community.name()).slug(community.slug()).build())
                .authorId(authorId)
                .author(UserRef.builder()
                        .id(authorId)
                        .username(by)
                        .build())
                .title(request.getTitle().trim())
                .content(request.getContent())
                .postType(type)
                .linkUrls(links)
                .media(media)
                .score(0L)
                .commentCount(0L)
                .status(status)
                .scheduledAt(scheduledAt)
                .createdAt(when)
                .updatedAt(when)
                .createdBy(by)
                .updatedBy(by)
                .build();
        post = postRepository.save(post);
        log.info("Post created id={} type={} status={} communityId={} authorId={}",
                post.getId(), type, status, post.getCommunityId(), authorId);

        String after = toJson(postMapper.toResponse(post));
        auditPort.record("CREATE", "POST", post.getId().toString(), authorId, by,
                "Post created: " + post.getTitle(), null, after);

        if (status == Post.Status.PUBLISHED) {
            String mediaUrl = media.stream().map(MediaRef::getUrl).filter(u -> u != null && !u.isBlank()).findFirst().orElse(null);
            notificationPort.notifyPostCreated(authorId, by, post.getId(), post.getTitle(), community.slug(),
                    request.getNotificationSummary(), mediaUrl);
            invalidateFeedCaches();
        }
        return postMapper.toResponse(post);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse get(UUID id) {
        RedisJsonCache cache = cache();
        if (cache != null) {
            var hit = cache.get(detailKey(id), PostResponse.class);
            if (hit.isPresent()) {
                return hit.get();
            }
        }
        PostResponse response = postMapper.toResponse(find(id));
        if (cache != null) {
            cache.set(detailKey(id), response, DETAIL_TTL);
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> feed(String sort, UUID communityId, UUID authorId, UUID userId, String q, Pageable pageable) {
        Pageable page = capped(pageable);
        String mode = sort == null ? "for_you" : sort.toLowerCase(Locale.ROOT).trim();

        if (q != null && !q.isBlank()) {
            return mapPage(search(q.trim(), applySort(mode, page)));
        }

        boolean cacheable = isAnonymousStyleFeed(mode, communityId, authorId, userId);
        RedisJsonCache cache = cache();
        String feedKey = null;
        if (cacheable && cache != null) {
            feedKey = feedKey(mode, communityId, authorId, page);
            var hit = cache.get(feedKey, new TypeReference<CachedPage<PostResponse>>() {});
            if (hit.isPresent()) {
                return hit.get().toPage();
            }
        }

        Page<PostResponse> result = switch (mode) {
            case "for_you", "foryou", "recommended" -> mapPage(forYou(userId, page));
            case "trending" -> mapPage(trending(communityId, userId, page));
            case "hot" -> mapPage(hotFeed(communityId, authorId, userId, page));
            case "top" -> mapPage(plainFeed(communityId, authorId, userId,
                    applySort("top", page)));
            default -> mapPage(plainFeed(communityId, authorId, userId,
                    applySort("new", page)));
        };

        if (feedKey != null && cache != null) {
            cache.set(feedKey, CachedPage.from(result), feedTtl(mode));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> related(UUID postId, int limit) {
        Post post = find(postId);
        int size = Math.min(Math.max(limit, 1), 20);
        String terms = titleTerms(post.getTitle());
        return postRepository.findRelated(post.getCommunityId(), postId, terms, size).stream()
                .map(postMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID userId) {
        Post post = find(id);
        boolean owner = post.getAuthorId().equals(userId);
        if (!owner && !AccessControl.canModerateContent()) {
            throw new ApiException("Not allowed to delete this post", HttpStatus.FORBIDDEN.value());
        }
        String before = toJson(postMapper.toResponse(post));
        postRepository.delete(post);
        auditPort.record("DELETE", "POST", id.toString(), userId, null,
                "Post deleted id=" + id, before, null);
        invalidateFeedCaches();
        RedisJsonCache cache = cache();
        if (cache != null) {
            cache.delete(detailKey(id));
        }
        log.info("Post deleted id={} by userId={} moderated={}", id, userId, !owner);
    }

    @Override
    @Transactional
    public PostResponse adjustScore(UUID id, long delta) {
        Post post = find(id);
        String before = toJson(postMapper.toResponse(post));
        post.setScore(post.getScore() + delta);
        post.setUpdatedBy("system");
        post = postRepository.save(post);
        auditPort.record("UPDATE", "POST", id.toString(), null, "system",
                "Score delta " + delta, before, toJson(postMapper.toResponse(post)));
        return postMapper.toResponse(post);
    }

    @Override
    @Transactional
    public PostResponse adjustCommentCount(UUID id, long delta) {
        Post post = find(id);
        post.setCommentCount(Math.max(0, post.getCommentCount() + delta));
        post.setUpdatedBy("system");
        return postMapper.toResponse(postRepository.save(post));
    }

    private Page<Post> search(String q, Pageable pageable) {
        try {
            Page<Post> fts = postRepository.searchByTitleFts(q, unsorted(pageable));
            if (!fts.isEmpty() || q.length() < 2) {
                return fts;
            }
        } catch (Exception e) {
            log.warn("FTS search failed, falling back to LIKE: {}", e.getMessage());
        }
        return postRepository.searchByTitleLike(q, pageable);
    }

    private Page<Post> forYou(UUID userId, Pageable pageable) {
        if (userId == null) {
            return trending(null, null, pageable);
        }
        List<UUID> joined = communityPort.findJoinedCommunityIds(userId);
        List<UUID> following = userPort.findFollowingIds(userId);
        if (joined.isEmpty() && following.isEmpty()) {
            return trending(null, userId, pageable);
        }
        return postRepository.findForYou(PgUuidArray.of(joined), PgUuidArray.of(following), unsorted(pageable));
    }

    private Page<Post> trending(UUID communityId, UUID userId, Pageable pageable) {
        Instant since = Instant.now().minus(TRENDING_HOURS, ChronoUnit.HOURS);
        Pageable p = unsorted(pageable);
        if (communityId != null) {
            return postRepository.findTrendingInCommunities(since, PgUuidArray.of(List.of(communityId)), p);
        }
        if (userId != null) {
            List<UUID> joined = communityPort.findJoinedCommunityIds(userId);
            if (!joined.isEmpty()) {
                Page<Post> scoped = postRepository.findTrendingInCommunities(since, PgUuidArray.of(joined), p);
                if (!scoped.isEmpty()) {
                    return scoped;
                }
            }
        }
        return postRepository.findTrending(since, p);
    }

    private Page<Post> hotFeed(UUID communityId, UUID authorId, UUID userId, Pageable pageable) {
        Pageable p = unsorted(pageable);
        if (communityId != null) {
            return postRepository.findByCommunityIdHot(communityId, p);
        }
        if (authorId != null) {
            return postRepository.findByAuthorIdHot(authorId, p);
        }
        if (userId != null) {
            List<UUID> joined = communityPort.findJoinedCommunityIds(userId);
            if (!joined.isEmpty()) {
                return postRepository.findByCommunityIdsHot(PgUuidArray.of(joined), p);
            }
        }
        return postRepository.findAllByHotRank(p);
    }

    private Page<Post> plainFeed(UUID communityId, UUID authorId, UUID userId, Pageable sorted) {
        if (communityId != null) {
            return postRepository.findByCommunityId(communityId, sorted);
        }
        if (authorId != null) {
            return postRepository.findByAuthorId(authorId, sorted);
        }
        if (userId != null) {
            List<UUID> joined = communityPort.findJoinedCommunityIds(userId);
            if (!joined.isEmpty()) {
                return postRepository.findByCommunityIdIn(joined, sorted);
            }
        }
        return postRepository.findAllVisible(sorted);
    }

    private Page<PostResponse> mapPage(Page<Post> page) {
        return page.map(postMapper::toResponse);
    }

    private Post find(UUID id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ApiException("Post not found", HttpStatus.NOT_FOUND.value()));
    }

    private Pageable applySort(String sort, Pageable pageable) {
        String s = sort == null ? "new" : sort.toLowerCase(Locale.ROOT);
        return switch (s) {
            case "top" -> PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "score").and(Sort.by(Sort.Direction.DESC, "createdAt")));
            case "hot", "trending", "for_you", "foryou", "recommended" -> unsorted(pageable);
            default -> PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
        };
    }

    private static Pageable capped(Pageable pageable) {
        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        int number = Math.max(pageable.getPageNumber(), 0);
        return PageRequest.of(number, size, pageable.getSort());
    }

    private static Pageable unsorted(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    private static String titleTerms(String title) {
        if (title == null || title.isBlank()) {
            return "post";
        }
        String cleaned = title.replaceAll("[^\\p{L}\\p{N}\\s]", " ").trim();
        if (cleaned.isBlank()) {
            return "post";
        }
        // plainto_tsquery handles multi-word phrases; keep a short signal string
        return cleaned.length() > 120 ? cleaned.substring(0, 120) : cleaned;
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    private RedisJsonCache cache() {
        return cacheEnabled ? cacheProvider.getIfAvailable() : null;
    }

    /** Cache only global / community / author feeds — not membership-scoped or for_you. */
    private static boolean isAnonymousStyleFeed(String mode, UUID communityId, UUID authorId, UUID userId) {
        if ("for_you".equals(mode) || "foryou".equals(mode) || "recommended".equals(mode)) {
            return false;
        }
        if (communityId != null || authorId != null) {
            return true;
        }
        // Global hot/new/top/trending without personalization
        return userId == null;
    }

    private static Duration feedTtl(String mode) {
        return switch (mode) {
            case "hot", "trending" -> Duration.ofSeconds(45);
            case "top" -> Duration.ofSeconds(90);
            default -> Duration.ofSeconds(20);
        };
    }

    private static String feedKey(String sort, UUID communityId, UUID authorId, Pageable page) {
        return "post:feed:v1:" + sort
                + ":" + (communityId == null ? "a" : communityId)
                + ":" + (authorId == null ? "a" : authorId)
                + ":p" + page.getPageNumber()
                + ":s" + page.getPageSize();
    }

    private static String detailKey(UUID id) {
        return "post:detail:v1:" + id;
    }

    private void invalidateFeedCaches() {
        RedisJsonCache cache = cache();
        if (cache != null) {
            cache.deleteByPrefix("post:feed:v1:");
        }
    }
}
