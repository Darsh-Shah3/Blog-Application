package com.threadly.comment.port;

import java.util.Optional;
import java.util.UUID;

public interface PostPort {

    Optional<PostSummary> findById(UUID postId);

    void ensureExists(UUID postId);

    void adjustCommentCount(UUID postId, long delta);
}
