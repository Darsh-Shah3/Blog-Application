package com.threadly.community.service;

import com.threadly.community.dto.CommunityResponse;
import com.threadly.community.dto.CreateCommunityRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CommunityService {

    CommunityResponse create(UUID userId, String actorUsername, CreateCommunityRequest request);

    CommunityResponse getBySlug(String slug, UUID viewerId);

    CommunityResponse getById(UUID id, UUID viewerId);

    Page<CommunityResponse> list(String q, UUID viewerId, Pageable pageable);

    Page<CommunityResponse> discover(UUID viewerId, Pageable pageable);

    CommunityResponse join(UUID communityId, UUID userId, String username);

    CommunityResponse leave(UUID communityId, UUID userId);

    List<UUID> joinedCommunityIds(UUID userId);

    Page<UUID> joinedCommunityIds(UUID userId, Pageable pageable);

    Page<UUID> memberIds(UUID communityId, Pageable pageable);

    void delete(UUID communityId, UUID userId);
}
