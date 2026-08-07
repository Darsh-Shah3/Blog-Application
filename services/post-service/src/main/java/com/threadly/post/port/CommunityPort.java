package com.threadly.post.port;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port to community-service.
 * Post domain does not depend on HTTP/WebClient types — only this contract.
 */
public interface CommunityPort {

    Optional<CommunitySummary> findById(Long communityId);

    List<Long> findJoinedCommunityIds(Long userId);
}
