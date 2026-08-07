package com.threadly.community.service;

import com.threadly.community.dto.CommunityResponse;
import com.threadly.community.dto.CreateCommunityRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/** Community use-cases; keep HTTP layer unaware of JPA entities. */
public interface CommunityService {

    CommunityResponse create(Long userId, String actorUsername, CreateCommunityRequest request);

    CommunityResponse getBySlug(String slug, Long viewerId);

    CommunityResponse getById(Long id, Long viewerId);

    Page<CommunityResponse> list(String q, Long viewerId, Pageable pageable);

    CommunityResponse join(Long communityId, Long userId);

    CommunityResponse leave(Long communityId, Long userId);

    List<Long> joinedCommunityIds(Long userId);

    List<Long> memberIds(Long communityId);

    void delete(Long communityId, Long userId);
}
