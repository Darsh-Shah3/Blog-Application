package com.threadly.audit.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Queries Prometheus HTTP API so admin UI can show live service graphs without opening Grafana.
 * <p>
 * Important: PromQL uses {@code {}} and {@code []}. Spring's UriBuilder treats braces as URI-template
 * variables, which silently drops or breaks range queries. Always build absolute encoded URIs.
 */
@Component
public class PrometheusClient {

    private static final Logger log = LoggerFactory.getLogger(PrometheusClient.class);

    private final WebClient webClient;
    private final String baseUrl;
    private final boolean enabled;

    public PrometheusClient(
            WebClient.Builder builder,
            @Value("${app.prometheus-base-url:http://localhost:9090}") String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        this.baseUrl = normalized;
        this.webClient = builder.baseUrl(normalized).build();
        this.enabled = !normalized.isBlank();
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Instant vector: metric name -> list of labeled samples. */
    public List<MetricSample> query(String promQl) {
        if (!enabled) {
            return List.of();
        }
        try {
            URI uri = URI.create(baseUrl + "/api/v1/query?query=" + enc(promQl));
            JsonNode root = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(5));
            return parseVector(root);
        } catch (Exception e) {
            log.warn("Prometheus query failed: {} — {}", promQl, e.getMessage());
            return List.of();
        }
    }

    public SeriesSeries queryRange(String promQl, Instant start, Instant end, String step) {
        if (!enabled) {
            return new SeriesSeries(promQl, List.of());
        }
        try {
            String url = baseUrl
                    + "/api/v1/query_range?query=" + enc(promQl)
                    + "&start=" + start.getEpochSecond()
                    + "&end=" + end.getEpochSecond()
                    + "&step=" + enc(step);
            URI uri = URI.create(url);
            JsonNode root = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(8));
            return parseMatrix(promQl, root);
        } catch (Exception e) {
            log.warn("Prometheus range query failed: {} — {}", promQl, e.getMessage());
            return new SeriesSeries(promQl, List.of());
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private List<MetricSample> parseVector(JsonNode root) {
        List<MetricSample> out = new ArrayList<>();
        if (root == null || !"success".equals(text(root, "status"))) {
            if (root != null) {
                log.warn("Prometheus query error: {}", root.path("error").asText("unknown"));
            }
            return out;
        }
        JsonNode result = root.path("data").path("result");
        if (!result.isArray()) {
            return out;
        }
        for (JsonNode series : result) {
            String application = label(series, "application", "job", "instance");
            String status = series.path("metric").path("status").asText(null);
            JsonNode value = series.path("value");
            if (value.isArray() && value.size() >= 2) {
                double v = parseDouble(value.get(1).asText("0"));
                if (Double.isFinite(v)) {
                    out.add(new MetricSample(application, status, v,
                            Instant.ofEpochSecond(value.get(0).asLong())));
                }
            }
        }
        return out;
    }

    private SeriesSeries parseMatrix(String query, JsonNode root) {
        List<LabeledSeries> seriesList = new ArrayList<>();
        if (root == null || !"success".equals(text(root, "status"))) {
            if (root != null) {
                log.warn("Prometheus range error: {}", root.path("error").asText("unknown"));
            }
            return new SeriesSeries(query, seriesList);
        }
        JsonNode result = root.path("data").path("result");
        if (!result.isArray()) {
            return new SeriesSeries(query, seriesList);
        }
        for (JsonNode series : result) {
            String application = label(series, "application", "job", "instance");
            List<TimePoint> points = new ArrayList<>();
            JsonNode values = series.path("values");
            if (values.isArray()) {
                for (JsonNode pair : values) {
                    if (pair.isArray() && pair.size() >= 2) {
                        double v = parseDouble(pair.get(1).asText("0"));
                        if (Double.isFinite(v)) {
                            points.add(new TimePoint(
                                    Instant.ofEpochSecond(pair.get(0).asLong()), v));
                        }
                    }
                }
            }
            if (!points.isEmpty()) {
                seriesList.add(new LabeledSeries(application, points));
            }
        }
        return new SeriesSeries(query, seriesList);
    }

    private static double parseDouble(String s) {
        if (s == null || s.isBlank() || "NaN".equalsIgnoreCase(s) || "+Inf".equals(s) || "-Inf".equals(s)) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static String label(JsonNode series, String... keys) {
        for (String key : keys) {
            String v = series.path("metric").path(key).asText(null);
            if (v != null && !v.isBlank() && !"unknown".equals(v)) {
                return v;
            }
        }
        return "unknown";
    }

    private static String text(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return n == null || n.isNull() ? null : n.asText();
    }

    public record MetricSample(String application, String status, double value, Instant ts) {}

    public record TimePoint(Instant t, double v) {}

    public record LabeledSeries(String label, List<TimePoint> points) {}

    public record SeriesSeries(String query, List<LabeledSeries> series) {}
}
