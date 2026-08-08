package com.threadly.post.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunityPort {

    Optional<CommunitySummary> findById(UUID communityId);

    List<UUID> findJoinedCommunityIds(UUID userId);
}
