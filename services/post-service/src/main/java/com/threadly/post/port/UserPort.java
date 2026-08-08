package com.threadly.post.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound port to user-auth-service. */
public interface UserPort {

    Optional<UserSummary> findById(UUID userId);

    /** User IDs the given user is following (empty if unknown / not found). */
    List<UUID> findFollowingIds(UUID userId);
}
