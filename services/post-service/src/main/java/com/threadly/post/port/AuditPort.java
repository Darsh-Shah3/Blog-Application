package com.threadly.post.port;

public interface AuditPort {
    void record(String action, String resourceType, String resourceId,
                java.util.UUID actorUserId, String actorUsername, String summary,
                String beforeData, String afterData);
}
