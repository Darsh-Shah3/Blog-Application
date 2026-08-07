package com.threadly.notification.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class AuthClient {

    private static final Logger log = LoggerFactory.getLogger(AuthClient.class);

    private final WebClient webClient;
    private final String internalApiKey;

    public AuthClient(
            WebClient.Builder builder,
            @Value("${app.services.auth-base-url}") String authBaseUrl,
            @Value("${app.internal-api-key}") String internalApiKey) {
        this.webClient = builder.baseUrl(authBaseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    public List<FollowerContact> listFollowers(Long userId) {
        try {
            return webClient.get()
                    .uri("/api/v1/internal/users/{id}/followers", userId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<FollowerContact>>() {})
                    .block(Duration.ofSeconds(5));
        } catch (Exception e) {
            log.warn("Failed to load followers userId={}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public record FollowerContact(Long id, String username, String email) {}
}
