package com.threadly.audit.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AuditEventResponse {
    private Long id;
    private Instant occurredAt;
    private String serviceName;
    private String action;
    private String resourceType;
    private String resourceId;
    private Long actorUserId;
    private String actorUsername;
    private String summary;
    private String metadata;
    private String requestId;
}
