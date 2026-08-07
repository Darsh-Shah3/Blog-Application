package com.threadly.audit.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * Best-effort live counts from domain services for admin reports.
 */
@Component
public class StatsClient {

    private static final Logger log = LoggerFactory.getLogger(StatsClient.class);

    private final WebClient.Builder builder;
    private final String internalApiKey;
    private final String authUrl;
    private final String postUrl;
    private final String communityUrl;
    private final String commentUrl;

    public StatsClient(
            WebClient.Builder builder,
            @Value("${app.internal-api-key}") String internalApiKey,
            @Value("${app.services.auth-base-url}") String authUrl,
            @Value("${app.services.post-base-url}") String postUrl,
            @Value("${app.services.community-base-url}") String communityUrl,
            @Value("${app.services.comment-base-url}") String commentUrl) {
        this.builder = builder;
        this.internalApiKey = internalApiKey;
        this.authUrl = authUrl;
        this.postUrl = postUrl;
        this.communityUrl = communityUrl;
        this.commentUrl = commentUrl;
    }

    public PlatformCounts fetchPlatformCounts() {
        return new PlatformCounts(
                count(authUrl, "/api/v1/internal/stats/users"),
                count(postUrl, "/api/v1/internal/stats/posts"),
                count(communityUrl, "/api/v1/internal/stats/communities"),
                count(commentUrl, "/api/v1/internal/stats/comments")
        );
    }

    @SuppressWarnings("unchecked")
    private long count(String base, String path) {
        try {
            Map<String, Object> body = builder.build().get()
                    .uri(base + path)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(3));
            if (body == null || body.get("count") == null) {
                return 0L;
            }
            return ((Number) body.get("count")).longValue();
        } catch (Exception e) {
            log.warn("Stats fetch failed {}{}: {}", base, path, e.getMessage());
            return 0L;
        }
    }

    public record PlatformCounts(long users, long posts, long communities, long comments) {}
}
