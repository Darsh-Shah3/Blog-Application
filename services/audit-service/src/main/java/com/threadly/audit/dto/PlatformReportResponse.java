package com.threadly.audit.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PlatformReportResponse {
    private long totalAuditEvents;
    private long auditEventsLast24h;
    private long auditEventsLast7d;
    private Map<String, Long> actionsLast7d;
    private Map<String, Long> servicesLast7d;
    private List<ResourceActionCount> resourceActionsLast7d;
    private long activeUsers;
    private long totalPosts;
    private long totalCommunities;
    private long totalComments;
    private String generatedAt;

    @Data
    @Builder
    public static class ResourceActionCount {
        private String resourceType;
        private String action;
        private long count;
    }
}
