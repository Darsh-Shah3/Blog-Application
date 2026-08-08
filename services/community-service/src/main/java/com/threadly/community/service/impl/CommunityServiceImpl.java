package com.threadly.community.service.impl;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private static final Logger log = LoggerFactory.getLogger(CommunityServiceImpl.class);
    private static final int MAX_PAGE_SIZE = 50;

    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;
    private final CommunityMapper communityMapper;
    private final SlugGenerator slugGenerator;

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
        log.info("Community created id={} slug={} creator={}", community.getId(), slug, userId);
        return communityMapper.toResponse(community, true);
    }

    @Override
    @Transactional(readOnly = true)
    public CommunityResponse getBySlug(String slug, UUID viewerId) {
        Community community = communityRepository.findBySlug(slug)
                .orElseThrow(() -> new ApiException("Community not found", HttpStatus.NOT_FOUND.value()));
        return communityMapper.toResponse(community, isJoined(community.getId(), viewerId));
    }

    @Override
    @Transactional(readOnly = true)
    public CommunityResponse getById(UUID id, UUID viewerId) {
        Community community = communityRepository.findById(id)
                .orElseThrow(() -> new ApiException("Community not found", HttpStatus.NOT_FOUND.value()));
        return communityMapper.toResponse(community, isJoined(id, viewerId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommunityResponse> list(String q, UUID viewerId, Pageable pageable) {
        Pageable page = capped(pageable);
        Page<Community> result = (q == null || q.isBlank())
                ? communityRepository.findAll(page)
                : communityRepository.findByNameContainingIgnoreCase(q.trim(), page);
        return mapWithJoined(result, viewerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommunityResponse> discover(UUID viewerId, Pageable pageable) {
        Pageable page = PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return mapWithJoined(communityRepository.findDiscover(page), viewerId);
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

    private static Pageable capped(Pageable pageable) {
        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        int number = Math.max(pageable.getPageNumber(), 0);
        return PageRequest.of(number, size, pageable.getSort());
    }
}
