package com.threadly.comment.port;

import java.util.Optional;

public interface UserPort {
    Optional<UserSummary> findById(java.util.UUID userId);
}
