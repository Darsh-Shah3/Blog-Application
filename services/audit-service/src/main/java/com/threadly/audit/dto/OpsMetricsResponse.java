package com.threadly.audit.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class OpsMetricsResponse {
    private boolean prometheusAvailable;
    private Instant generatedAt;
    /** Request rate (req/s) by application. */
    private Map<String, Double> requestRateByService;
    /** p95 latency seconds by application. */
    private Map<String, Double> p95LatencySecondsByService;
    /** 5xx rate (req/s) by application. */
    private Map<String, Double> error5xxRateByService;
    /** JVM heap used bytes by application. */
    private Map<String, Double> heapUsedBytesByService;
    /** Time-series charts for the admin UI. */
    private List<ChartSeries> charts;

    @Data
    @Builder
    public static class ChartSeries {
        private String id;
        private String title;
        private String unit;
        private List<Line> lines;
    }

    @Data
    @Builder
    public static class Line {
        private String label;
        private List<Point> points;
    }

    @Data
    @Builder
    public static class Point {
        private long t;
        private double v;
    }
}
