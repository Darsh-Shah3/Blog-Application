package com.threadly.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private UUID userId;
    private Map<String, Object> recipient;
    private String type;
    private String title;
    private String body;
    private String summary;
    private String linkUrl;
    private String mediaUrl;
    private String actorUsername;
    private String postTitle;
    private String resourceType;
    private String resourceId;
    private Instant readAt;
    private Instant createdAt;
    private boolean read;
}
