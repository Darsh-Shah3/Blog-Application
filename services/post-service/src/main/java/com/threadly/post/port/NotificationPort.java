package com.threadly.post.port;

import java.util.UUID;

public interface NotificationPort {
    void notifyPostCreated(UUID authorId, String authorUsername, UUID postId, String title,
                           String communitySlug, String summary, String mediaUrl);
}
