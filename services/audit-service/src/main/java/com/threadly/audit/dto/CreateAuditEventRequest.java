package com.threadly.audit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAuditEventRequest {
    @NotBlank
    private String serviceName;
    @NotBlank
    private String action;
    @NotBlank
    private String resourceType;
    private String resourceId;
    private Long actorUserId;
    private String actorUsername;
    private String summary;
    private String metadata;
    private String requestId;
}
