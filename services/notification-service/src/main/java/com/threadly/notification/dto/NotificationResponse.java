package com.threadly.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String body;
    private String linkUrl;
    private String actorUsername;
    private String resourceType;
    private String resourceId;
    private Instant readAt;
    private Instant createdAt;
    private boolean read;
}
