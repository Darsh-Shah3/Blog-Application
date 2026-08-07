package com.threadly.comment.port;

/** Verifies post existence and updates counters without coupling to HTTP types. */
public interface PostPort {

    void ensureExists(Long postId);

    void adjustCommentCount(Long postId, long delta);
}
