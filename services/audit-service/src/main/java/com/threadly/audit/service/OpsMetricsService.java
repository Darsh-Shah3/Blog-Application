package com.threadly.audit.service;

import com.threadly.audit.client.PrometheusClient;
import com.threadly.audit.dto.OpsMetricsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pulls live graphs from Prometheus for the admin Analytics page.
 * <p>
 * Note: Spring Boot default Prometheus export is Summary-style
 * ({@code *_count}/{@code *_sum}/{@code *_max}), not histogram buckets — so classic
 * {@code histogram_quantile} p95 returns empty. We use average latency from sum/count instead.
 */
@Service
@RequiredArgsConstructor
public class OpsMetricsService {

    private static final String RPS =
            "sum(rate(http_server_requests_seconds_count[1m])) by (application)";
    /** Mean request duration ≈ rate(sum) / rate(count). */
    private static final String AVG_LATENCY =
            "sum(rate(http_server_requests_seconds_sum[5m])) by (application)"
                    + " / sum(rate(http_server_requests_seconds_count[5m])) by (application)";
    private static final String ERR5XX =
            "sum(rate(http_server_requests_seconds_count{status=~\"5..\"}[1m])) by (application)";
    private static final String HEAP =
            "sum(jvm_memory_used_bytes{area=\"heap\"}) by (application)";
    private static final String RPS_APP =
            "sum(rate(http_server_requests_seconds_count{uri!~\"/actuator.*\"}[1m])) by (application)";

    private final PrometheusClient prometheusClient;

    public OpsMetricsResponse snapshot(int hours) {
        int windowHours = Math.min(Math.max(hours, 1), 24);
        Instant end = Instant.now();
        Instant start = end.minus(windowHours, ChronoUnit.HOURS);
        String step = windowHours <= 3 ? "30s" : windowHours <= 12 ? "1m" : "2m";

        boolean available = prometheusClient.isEnabled();
        List<PrometheusClient.MetricSample> upSamples = prometheusClient.query("up");
        if (available && upSamples.isEmpty()) {
            available = false;
        } else if (!upSamples.isEmpty()) {
            available = upSamples.stream().anyMatch(s -> s.value() >= 1.0);
        }

        Map<String, Double> rps = toMap(prometheusClient.query(RPS));
        Map<String, Double> rpsApp = toMap(prometheusClient.query(RPS_APP));
        if (!rpsApp.isEmpty()) {
            rps = rpsApp;
        }
        Map<String, Double> latency = toMap(prometheusClient.query(AVG_LATENCY));
        Map<String, Double> err = toMap(prometheusClient.query(ERR5XX));
        Map<String, Double> heap = toMap(prometheusClient.query(HEAP));

        if (rps.isEmpty() && heap.isEmpty() && !upSamples.isEmpty()) {
            for (PrometheusClient.MetricSample s : upSamples) {
                if (s.value() >= 1.0) {
                    heap.putIfAbsent(s.application(), 0.0);
                }
            }
        }

        String rpsQuery = rpsApp.isEmpty() ? RPS : RPS_APP;

        List<OpsMetricsResponse.ChartSeries> charts = new ArrayList<>();
        charts.add(toChart("rps", "HTTP request rate", "req/s",
                prometheusClient.queryRange(rpsQuery, start, end, step)));
        charts.add(toChart("latency", "HTTP avg latency", "s",
                prometheusClient.queryRange(AVG_LATENCY, start, end, step)));
        charts.add(toChart("errors", "HTTP 5xx rate", "req/s",
                prometheusClient.queryRange(ERR5XX, start, end, step)));
        charts.add(toChart("heap", "JVM heap used", "bytes",
                prometheusClient.queryRange(HEAP, start, end, step)));

        return OpsMetricsResponse.builder()
                .prometheusAvailable(available)
                .generatedAt(Instant.now())
                .requestRateByService(rps)
                .p95LatencySecondsByService(latency)
                .error5xxRateByService(err)
                .heapUsedBytesByService(heap)
                .charts(charts)
                .build();
    }

    private static Map<String, Double> toMap(List<PrometheusClient.MetricSample> samples) {
        Map<String, Double> map = new LinkedHashMap<>();
        for (PrometheusClient.MetricSample s : samples) {
            if (Double.isFinite(s.value()) && s.application() != null) {
                map.put(s.application(), s.value());
            }
        }
        return map;
    }

    private static OpsMetricsResponse.ChartSeries toChart(
            String id, String title, String unit, PrometheusClient.SeriesSeries series) {
        List<OpsMetricsResponse.Line> lines = new ArrayList<>();
        for (PrometheusClient.LabeledSeries ls : series.series()) {
            List<OpsMetricsResponse.Point> points = ls.points().stream()
                    .filter(p -> Double.isFinite(p.v()))
                    .map(p -> OpsMetricsResponse.Point.builder()
                            .t(p.t().getEpochSecond())
                            .v(p.v())
                            .build())
                    .toList();
            if (!points.isEmpty()) {
                lines.add(OpsMetricsResponse.Line.builder()
                        .label(ls.label())
                        .points(points)
                        .build());
            }
        }
        return OpsMetricsResponse.ChartSeries.builder()
                .id(id)
                .title(title)
                .unit(unit)
                .lines(lines)
                .build();
    }
}
