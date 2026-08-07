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
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private static final Logger log = LoggerFactory.getLogger(CommunityServiceImpl.class);

    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;
    private final CommunityMapper communityMapper;
    private final SlugGenerator slugGenerator;

    @Override
    @Transactional
    public CommunityResponse create(Long userId, String actorUsername, CreateCommunityRequest request) {
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
                .memberCount(0L)
                .createdBy(by)
                .updatedBy(by)
                .build();
        community = communityRepository.save(community);
        // Creator is auto-joined so home-feed personalization works immediately.
        joinInternal(community, userId, by);
        log.info("Community created id={} slug={} creator={} createdBy={}", community.getId(), slug, userId, by);
        return communityMapper.toResponse(community, true);
    }

    @Override
    @Transactional(readOnly = true)
    public CommunityResponse getBySlug(String slug, Long viewerId) {
        Community community = communityRepository.findBySlug(slug)
                .orElseThrow(() -> new ApiException("Community not found", HttpStatus.NOT_FOUND.value()));
        return communityMapper.toResponse(community, isJoined(community.getId(), viewerId));
    }

    @Override
    @Transactional(readOnly = true)
    public CommunityResponse getById(Long id, Long viewerId) {
        Community community = communityRepository.findById(id)
                .orElseThrow(() -> new ApiException("Community not found", HttpStatus.NOT_FOUND.value()));
        return communityMapper.toResponse(community, isJoined(id, viewerId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommunityResponse> list(String q, Long viewerId, Pageable pageable) {
        Page<Community> page = (q == null || q.isBlank())
                ? communityRepository.findAll(pageable)
                : communityRepository.findByNameContainingIgnoreCase(q.trim(), pageable);
        return page.map(c -> communityMapper.toResponse(c, isJoined(c.getId(), viewerId)));
    }

    @Override
    @Transactional
    public CommunityResponse join(Long communityId, Long userId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ApiException("Community not found", HttpStatus.NOT_FOUND.value()));
        if (membershipRepository.existsByCommunityIdAndUserId(communityId, userId)) {
            return communityMapper.toResponse(community, true);
        }
        joinInternal(community, userId, "user-" + userId);
        return communityMapper.toResponse(community, true);
    }

    @Override
    @Transactional
    public CommunityResponse leave(Long communityId, Long userId) {
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
    public List<Long> joinedCommunityIds(Long userId) {
        return membershipRepository.findByUserId(userId).stream().map(Membership::getCommunityId).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> memberIds(Long communityId) {
        if (!communityRepository.existsById(communityId)) {
            throw new ApiException("Community not found", HttpStatus.NOT_FOUND.value());
        }
        return membershipRepository.findByCommunityId(communityId).stream().map(Membership::getUserId).toList();
    }

    @Override
    @Transactional
    public void delete(Long communityId, Long userId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ApiException("Community not found", HttpStatus.NOT_FOUND.value()));
        boolean creator = community.getCreatorId().equals(userId);
        if (!creator && !AccessControl.canDeleteCommunity()) {
            throw new ApiException("Not allowed to delete this community", HttpStatus.FORBIDDEN.value());
        }
        membershipRepository.deleteByCommunityId(communityId);
        communityRepository.delete(community);
        log.info("Community deleted id={} by userId={} moderated={}", communityId, userId, !creator);
    }

    private boolean isJoined(Long communityId, Long viewerId) {
        return viewerId != null && membershipRepository.existsByCommunityIdAndUserId(communityId, viewerId);
    }

    private void joinInternal(Community community, Long userId, String actorUsername) {
        membershipRepository.save(Membership.builder()
                .communityId(community.getId())
                .userId(userId)
                .build());
        community.setMemberCount(community.getMemberCount() + 1);
        community.setUpdatedBy(actorUsername != null ? actorUsername : "user-" + userId);
        communityRepository.save(community);
    }
}
