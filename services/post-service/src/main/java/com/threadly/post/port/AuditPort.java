package com.threadly.post.port;

public interface AuditPort {
    void record(String action, String resourceType, String resourceId,
                Long actorUserId, String actorUsername, String summary);
}
