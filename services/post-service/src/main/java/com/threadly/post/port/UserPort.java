package com.threadly.post.port;

import java.util.Optional;

/** Outbound port to user-auth-service. */
public interface UserPort {

    Optional<UserSummary> findById(Long userId);
}
