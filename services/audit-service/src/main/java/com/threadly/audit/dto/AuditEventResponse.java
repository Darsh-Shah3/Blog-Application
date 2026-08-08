package com.threadly.audit.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AuditEventResponse {
    private UUID id;
    private Instant occurredAt;
    private String serviceName;
    private String action;
    private String resourceType;
    private String resourceId;
    private UUID actorUserId;
    private String actorUsername;
    private String summary;
    private String metadata;
    private String beforeData;
    private String afterData;
    private String requestId;
}
