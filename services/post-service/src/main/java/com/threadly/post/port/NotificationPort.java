package com.threadly.post.port;

public interface NotificationPort {
    void notifyPostCreated(Long authorId, String authorUsername, Long postId, String title, String communitySlug);
}
