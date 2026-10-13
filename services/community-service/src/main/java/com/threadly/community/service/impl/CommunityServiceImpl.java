package com.threadly.community.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.threadly.community.cache.CachedPage;
import com.threadly.community.cache.RedisJsonCache;
import com.threadly.community.dto.CommunityResponse;
import com.threadly.community.dto.CreateCommunityRequest;
import com.threadly.community.entity.Community;
import com.threadly.community.entity.Membership;
import com.threadly.community.exception.ApiException;
import com.threadly.community.mapper.CommunityMapper;
import com.threadly.community.repository.CommunityRepository;
import com.threadly.community.repository.MembershipRepository;
import com.threadly.community.security.AccessControl;
import com.threadly.community.service.CommunityService;
import com.threadly.community.util.AuditActors;
import com.threadly.community.util.SlugGenerator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private static final Logger log = LoggerFactory.getLogger(CommunityServiceImpl.class);
    private static final int MAX_PAGE_SIZE = 50;
    private static final Duration DETAIL_TTL = Duration.ofMinutes(3);
    private static final Duration DISCOVER_TTL = Duration.ofSeconds(90);
    private static final Duration LIST_TTL = Duration.ofSeconds(60);

    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;
    private final CommunityMapper communityMapper;
    private final SlugGenerator slugGenerator;
    private final ObjectProvider<RedisJsonCache> cacheProvider;

    @Value("${app.cache.redis-enabled:false}")
    private boolean cacheEnabled;

    @Override
    @Transactional
    public CommunityResponse create(UUID userId, String actorUsername, CreateCommunityRequest request) {
        String slug = slugGenerator.fromName(request.getName());
        if (communityRepository.existsBySlug(slug)) {
            throw new ApiException("Community slug already exists: " + slug, HttpStatus.CONFLICT.value());
        }
        String by = AuditActors.resolve(actorUsername, userId);
        Community community = Community.builder()
                .name(request.getName().trim())
                .slug(slug)
                .description(request.getDescription())
                .creatorId(userId)
                .creator(Map.of("id", userId.toString(), "username", by))
                .memberCount(0L)
                .createdBy(by)
                .updatedBy(by)
                .build();
        community = communityRepository.save(community);
        joinInternal(community, userId, by);
        invalidateListCaches();
        log.info("Community created id={} slug={} creator={}", community.getId(), slug, userId);
        return communityMapper.toResponse(community, true);
    }

    @Override
    @Transactional(readOnly = true)
    public CommunityResponse getBySlug(String slug, UUID viewerId) {
        RedisJsonCache cache = cache();
        if (cache != null) {
            var hit = cache.get(slugKey(slug), CommunityResponse.class);
            if (hit.isPresent()) {
                CommunityResponse cached = hit.get();
                cached.setJoined(isJoined(cached.getId(), viewerId));
                return cached;
            }
        }
        Community community = communityRepository.findBySlug(slug)
                .orElseThrow(() -> new ApiException("Community not found", HttpStatus.NOT_FOUND.value()));
        CommunityResponse shared = communityMapper.toResponse(community, false);
        if (cache != null) {
            cache.set(slugKey(slug), shared, DETAIL_TTL);
            cache.set(idKey(community.getId()), shared, DETAIL_TTL);
        }
        shared.setJoined(isJoined(community.getId(), viewerId));
        return shared;
    }

    @Override
    @Transactional(readOnly = true)
    public CommunityResponse getById(UUID id, UUID viewerId) {
        RedisJsonCache cache = cache();
        if (cache != null) {
            var hit = cache.get(idKey(id), CommunityResponse.class);
            if (hit.isPresent()) {
                CommunityResponse cached = hit.get();
                cached.setJoined(isJoined(id, viewerId));
                return cached;
            }
        }
        Community community = communityRepository.findById(id)
                .orElseThrow(() -> new ApiException("Community not found", HttpStatus.NOT_FOUND.value()));
        CommunityResponse shared = communityMapper.toResponse(community, false);
        if (cache != null) {
            cache.set(idKey(id), shared, DETAIL_TTL);
            if (community.getSlug() != null) {
                cache.set(slugKey(community.getSlug()), shared, DETAIL_TTL);
            }
        }
        shared.setJoined(isJoined(id, viewerId));
        return shared;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommunityResponse> list(String q, UUID viewerId, Pageable pageable) {
        Pageable page = capped(pageable);
        String qKey = (q == null || q.isBlank()) ? "" : q.trim().toLowerCase();
        RedisJsonCache cache = cache();
        String key = "community:list:v1:" + qKey + ":p" + page.getPageNumber() + ":s" + page.getPageSize();
        if (cache != null) {
            var hit = cache.get(key, new TypeReference<CachedPage<CommunityResponse>>() {});
            if (hit.isPresent()) {
                return overlayJoined(hit.get().toPage(), viewerId);
            }
        }
        Page<Community> result = (q == null || q.isBlank())
                ? communityRepository.findAll(page)
                : communityRepository.findByNameContainingIgnoreCase(q.trim(), page);
        Page<CommunityResponse> mapped = mapWithJoined(result, null);
        if (cache != null) {
            cache.set(key, CachedPage.from(mapped), LIST_TTL);
        }
        return overlayJoined(mapped, viewerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommunityResponse> discover(UUID viewerId, Pageable pageable) {
        Pageable page = PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        RedisJsonCache cache = cache();
        String key = "community:discover:v1:p" + page.getPageNumber() + ":s" + page.getPageSize();
        if (cache != null) {
            var hit = cache.get(key, new TypeReference<CachedPage<CommunityResponse>>() {});
            if (hit.isPresent()) {
                return overlayJoined(hit.get().toPage(), viewerId);
            }
        }
        Page<CommunityResponse> mapped = mapWithJoined(communityRepository.findDiscover(page), null);
        if (cache != null) {
            cache.set(key, CachedPage.from(mapped), DISCOVER_TTL);
        }
        return overlayJoined(mapped, viewerId);
    }

    @Override
    @Transactional
    public CommunityResponse join(UUID communityId, UUID userId, String username) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ApiException("Community not found", HttpStatus.NOT_FOUND.value()));
        if (membershipRepository.existsByCommunityIdAndUserId(communityId, userId)) {
            return communityMapper.toResponse(community, true);
        }
        String by = AuditActors.resolve(username, userId);
        joinInternal(community, userId, by);
        invalidateCommunity(community);
        return communityMapper.toResponse(community, true);
    }

    @Override
    @Transactional
    public CommunityResponse leave(UUID communityId, UUID userId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ApiException("Community not found", HttpStatus.NOT_FOUND.value()));
        membershipRepository.findByCommunityIdAndUserId(communityId, userId).ifPresent(m -> {
            membershipRepository.delete(m);
            community.setMemberCount(Math.max(0, community.getMemberCount() - 1));
            community.setUpdatedBy("user-" + userId);
            communityRepository.save(community);
        });
        invalidateCommunity(community);
        return communityMapper.toResponse(community, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> joinedCommunityIds(UUID userId) {
        return membershipRepository.findByUserId(userId).stream().map(Membership::getCommunityId).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UUID> joinedCommunityIds(UUID userId, Pageable pageable) {
        return membershipRepository.findByUserId(userId, capped(pageable)).map(Membership::getCommunityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UUID> memberIds(UUID communityId, Pageable pageable) {
        if (!communityRepository.existsById(communityId)) {
            throw new ApiException("Community not found", HttpStatus.NOT_FOUND.value());
        }
        return membershipRepository.findByCommunityId(communityId, capped(pageable)).map(Membership::getUserId);
    }

    @Override
    @Transactional
    public void delete(UUID communityId, UUID userId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ApiException("Community not found", HttpStatus.NOT_FOUND.value()));
        boolean creator = community.getCreatorId().equals(userId);
        if (!creator && !AccessControl.canDeleteCommunity()) {
            throw new ApiException("Not allowed to delete this community", HttpStatus.FORBIDDEN.value());
        }
        membershipRepository.deleteByCommunityId(communityId);
        communityRepository.delete(community);
        invalidateCommunity(community);
        invalidateListCaches();
        log.info("Community deleted id={} by userId={}", communityId, userId);
    }

    private Page<CommunityResponse> mapWithJoined(Page<Community> page, UUID viewerId) {
        if (viewerId == null || page.isEmpty()) {
            return page.map(c -> communityMapper.toResponse(c, false));
        }
        List<UUID> ids = page.getContent().stream().map(Community::getId).toList();
        Set<UUID> joined = membershipRepository.findJoinedCommunityIds(viewerId, ids);
        return page.map(c -> communityMapper.toResponse(c, joined.contains(c.getId())));
    }

    private Page<CommunityResponse> overlayJoined(Page<CommunityResponse> page, UUID viewerId) {
        if (viewerId == null || page.isEmpty()) {
            page.forEach(c -> c.setJoined(false));
            return page;
        }
        List<UUID> ids = page.getContent().stream().map(CommunityResponse::getId).toList();
        Set<UUID> joined = membershipRepository.findJoinedCommunityIds(viewerId, ids);
        page.forEach(c -> c.setJoined(joined.contains(c.getId())));
        return page;
    }

    private boolean isJoined(UUID communityId, UUID viewerId) {
        return viewerId != null && membershipRepository.existsByCommunityIdAndUserId(communityId, viewerId);
    }

    private void joinInternal(Community community, UUID userId, String username) {
        membershipRepository.save(Membership.builder()
                .communityId(community.getId())
                .community(Map.of(
                        "id", community.getId().toString(),
                        "name", community.getName(),
                        "slug", community.getSlug()))
                .userId(userId)
                .member(Map.of("id", userId.toString(), "username", username == null ? "user" : username))
                .build());
        community.setMemberCount(community.getMemberCount() + 1);
        community.setUpdatedBy(username != null ? username : "user-" + userId);
        communityRepository.save(community);
    }

    private RedisJsonCache cache() {
        return cacheEnabled ? cacheProvider.getIfAvailable() : null;
    }

    private void invalidateCommunity(Community community) {
        RedisJsonCache cache = cache();
        if (cache == null || community == null) {
            return;
        }
        if (community.getId() != null) {
            cache.delete(idKey(community.getId()));
        }
        if (community.getSlug() != null) {
            cache.delete(slugKey(community.getSlug()));
        }
        invalidateListCaches();
    }

    private void invalidateListCaches() {
        RedisJsonCache cache = cache();
        if (cache != null) {
            cache.deleteByPrefix("community:list:v1:");
            cache.deleteByPrefix("community:discover:v1:");
        }
    }

    private static String slugKey(String slug) {
        return "community:slug:v1:" + slug;
    }

    private static String idKey(UUID id) {
        return "community:id:v1:" + id;
    }

    private static Pageable capped(Pageable pageable) {
        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        int number = Math.max(pageable.getPageNumber(), 0);
        return PageRequest.of(number, size, pageable.getSort());
    }
}
