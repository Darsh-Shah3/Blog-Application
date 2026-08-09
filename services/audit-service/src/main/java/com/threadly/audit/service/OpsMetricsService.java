package com.threadly.audit.service;

import com.threadly.audit.client.PrometheusClient;
import com.threadly.audit.dto.OpsMetricsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Live ops graphs from Prometheus for the admin infrastructure page.
 * Uses Summary-style metrics (count/sum) — no histogram buckets required.
 */
@Service
@RequiredArgsConstructor
public class OpsMetricsService {

    private static final String RPS =
            "sum(rate(http_server_requests_seconds_count[1m])) by (application)";
    private static final String RPS_APP =
            "sum(rate(http_server_requests_seconds_count{uri!~\"/actuator.*\"}[1m])) by (application)";
    private static final String AVG_LATENCY =
            "sum(rate(http_server_requests_seconds_sum[5m])) by (application)"
                    + " / sum(rate(http_server_requests_seconds_count[5m])) by (application)";
    /** Prefer Micrometer outcome; also match numeric status codes. */
    private static final String ERR4XX =
            "(sum(rate(http_server_requests_seconds_count{outcome=\"CLIENT_ERROR\"}[1m])) by (application))"
                    + " or (sum(rate(http_server_requests_seconds_count{status=~\"4..\"}[1m])) by (application))";
    private static final String ERR5XX =
            "(sum(rate(http_server_requests_seconds_count{outcome=\"SERVER_ERROR\"}[1m])) by (application))"
                    + " or (sum(rate(http_server_requests_seconds_count{status=~\"5..\"}[1m])) by (application))";
    private static final String HEAP =
            "sum(jvm_memory_used_bytes{area=\"heap\"}) by (application)";
    private static final String NON_HEAP =
            "sum(jvm_memory_used_bytes{area=\"nonheap\"}) by (application)";
    private static final String THREADS =
            "sum(jvm_threads_live_threads) by (application)";
    private static final String CPU =
            "avg(process_cpu_usage) by (application)";
    private static final String DB_ACTIVE =
            "sum(hikaricp_connections_active) by (application)";

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
        Map<String, Double> err4 = toMap(prometheusClient.query(ERR4XX));
        Map<String, Double> heap = toMap(prometheusClient.query(HEAP));
        Map<String, Double> cpu = toMap(prometheusClient.query(CPU));
        Map<String, Double> threads = toMap(prometheusClient.query(THREADS));

        String rpsQuery = rpsApp.isEmpty() ? RPS : RPS_APP;
        Set<String> knownApps = new LinkedHashSet<>();
        knownApps.addAll(rps.keySet());
        knownApps.addAll(heap.keySet());
        for (PrometheusClient.MetricSample s : upSamples) {
            if (s.value() >= 1.0 && s.application() != null) {
                knownApps.add(s.application());
            }
        }

        List<OpsMetricsResponse.ChartSeries> charts = new ArrayList<>();
        charts.add(toChart("rps", "HTTP request rate", "req/s",
                prometheusClient.queryRange(rpsQuery, start, end, step), knownApps, rps, start, end));
        charts.add(toChart("latency", "HTTP avg latency", "s",
                prometheusClient.queryRange(AVG_LATENCY, start, end, step), knownApps, latency, start, end));
        charts.add(toChart("errors4xx", "HTTP 4xx rate", "req/s",
                prometheusClient.queryRange(ERR4XX, start, end, step), knownApps, err4, start, end));
        charts.add(toChart("errors", "HTTP 5xx rate", "req/s",
                prometheusClient.queryRange(ERR5XX, start, end, step), knownApps, err, start, end));
        charts.add(toChart("heap", "JVM heap used", "bytes",
                prometheusClient.queryRange(HEAP, start, end, step), knownApps, heap, start, end));
        charts.add(toChart("nonheap", "JVM non-heap used", "bytes",
                prometheusClient.queryRange(NON_HEAP, start, end, step), knownApps,
                toMap(prometheusClient.query(NON_HEAP)), start, end));
        charts.add(toChart("threads", "JVM live threads", "count",
                prometheusClient.queryRange(THREADS, start, end, step), knownApps, threads, start, end));
        charts.add(toChart("cpu", "Process CPU usage", "ratio",
                prometheusClient.queryRange(CPU, start, end, step), knownApps, cpu, start, end));
        charts.add(toChart("db", "DB pool active connections", "count",
                prometheusClient.queryRange(DB_ACTIVE, start, end, step), knownApps,
                toMap(prometheusClient.query(DB_ACTIVE)), start, end));

        // Snapshot maps: zero-fill known errors so bars aren't blank when healthy
        if (available) {
            for (String app : knownApps) {
                err4.putIfAbsent(app, 0.0);
                err.putIfAbsent(app, 0.0);
            }
        }

        return OpsMetricsResponse.builder()
                .prometheusAvailable(available)
                .generatedAt(Instant.now())
                .requestRateByService(rps)
                .p95LatencySecondsByService(latency)
                .error5xxRateByService(err)
                .heapUsedBytesByService(heap)
                .error4xxRateByService(err4)
                .cpuUsageByService(cpu)
                .threadsByService(threads)
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
            String id,
            String title,
            String unit,
            PrometheusClient.SeriesSeries series,
            Set<String> knownApps,
            Map<String, Double> instant,
            Instant start,
            Instant end) {
        List<OpsMetricsResponse.Line> lines = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (PrometheusClient.LabeledSeries ls : series.series()) {
            List<OpsMetricsResponse.Point> points = ls.points().stream()
                    .filter(p -> Double.isFinite(p.v()))
                    .map(p -> OpsMetricsResponse.Point.builder()
                            .t(p.t().getEpochSecond())
                            .v(p.v())
                            .build())
                    .toList();
            if (!points.isEmpty()) {
                seen.add(ls.label());
                lines.add(OpsMetricsResponse.Line.builder()
                        .label(ls.label())
                        .points(points)
                        .build());
            }
        }

        boolean errorChart = "errors4xx".equals(id) || "errors".equals(id);

        // Healthy stacks often return no 4xx/5xx matrix rows — show flat zero lines.
        if (errorChart) {
            for (String app : knownApps) {
                if (seen.contains(app)) {
                    continue;
                }
                lines.add(constantLine(app, instant.getOrDefault(app, 0.0), start, end));
                seen.add(app);
            }
        }

        // If range parsing failed but instant gauges exist (heap/cpu), paint a constant line.
        if (lines.isEmpty() && instant != null && !instant.isEmpty()) {
            for (Map.Entry<String, Double> e : instant.entrySet()) {
                lines.add(constantLine(e.getKey(), e.getValue(), start, end));
            }
        }

        return OpsMetricsResponse.ChartSeries.builder()
                .id(id)
                .title(title)
                .unit(unit)
                .lines(lines)
                .build();
    }

    private static OpsMetricsResponse.Line constantLine(String label, double v, Instant start, Instant end) {
        double safe = Double.isFinite(v) ? v : 0.0;
        return OpsMetricsResponse.Line.builder()
                .label(label)
                .points(List.of(
                        OpsMetricsResponse.Point.builder().t(start.getEpochSecond()).v(safe).build(),
                        OpsMetricsResponse.Point.builder().t(end.getEpochSecond()).v(safe).build()
                ))
                .build();
    }
}
