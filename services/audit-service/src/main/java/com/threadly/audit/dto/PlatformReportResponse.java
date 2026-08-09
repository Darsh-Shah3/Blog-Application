package com.threadly.audit.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Admin platform analytics: totals + labeled series for in-app report charts.
 * Backed by audit_events + live counts from domain services.
 */
@Data
@Builder
public class PlatformReportResponse {
    private long totalAuditEvents;
    private long auditEventsLast24h;
    private long auditEventsLast7d;
    private Map<String, Long> actionsLast7d;
    private Map<String, Long> servicesLast7d;
    private Map<String, Long> resourceTypesLast7d;
    private List<ResourceActionCount> resourceActionsLast7d;
    private List<NameCount> topActorsLast7d;
    /** Daily audit volume for trend chart (filled with zeros for quiet days). */
    private List<DayCount> auditEventsPerDay;
    /** Hour-of-day (0–23 UTC) activity over the selected window. */
    private List<HourCount> activityByHourUtc;
    private long activeUsers;
    private long totalPosts;
    private long totalCommunities;
    private long totalComments;
    /** Window used for trend queries (days). */
    private int reportDays;
    private String generatedAt;

    @Data
    @Builder
    public static class ResourceActionCount {
        private String resourceType;
        private String action;
        private long count;
    }

    @Data
    @Builder
    public static class NameCount {
        private String name;
        private long count;
    }

    @Data
    @Builder
    public static class DayCount {
        private String day;
        private long count;
    }

    @Data
    @Builder
    public static class HourCount {
        private int hour;
        private long count;
    }
}
