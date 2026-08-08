package com.threadly.vote.client;

import com.threadly.vote.config.RemoteServiceProperties;
import com.threadly.vote.entity.Vote;
import com.threadly.vote.exception.ApiException;
import com.threadly.vote.port.ScoringPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Component
public class WebClientScoringAdapter implements ScoringPort {

    private static final Logger log = LoggerFactory.getLogger(WebClientScoringAdapter.class);

    private final WebClient postClient;
    private final WebClient commentClient;
    private final WebClient authClient;
    private final String internalApiKey;

    public WebClientScoringAdapter(WebClient.Builder builder, RemoteServiceProperties props) {
        this.postClient = builder.baseUrl(props.getPostBaseUrl()).build();
        this.commentClient = builder.baseUrl(props.getCommentBaseUrl()).build();
        this.authClient = builder.baseUrl(props.getAuthBaseUrl()).build();
        this.internalApiKey = props.getInternalApiKey();
    }

    @Override
    public UUID resolveAuthorId(Vote.TargetType type, UUID targetId) {
        TargetInfo info = fetch(type, targetId);
        return info != null ? info.authorId : null;
    }

    @Override
    public String resolveTargetName(Vote.TargetType type, UUID targetId) {
        TargetInfo info = fetch(type, targetId);
        if (info == null) return null;
        if (info.title != null) return info.title;
        if (info.content != null) {
            return info.content.length() <= 80 ? info.content : info.content.substring(0, 80) + "…";
        }
        return type.name() + " " + targetId;
    }

    private TargetInfo fetch(Vote.TargetType type, UUID targetId) {
        try {
            if (type == Vote.TargetType.POST) {
                return postClient.get()
                        .uri("/api/v1/posts/{id}", targetId)
                        .header("X-Internal-Api-Key", internalApiKey)
                        .retrieve()
                        .bodyToMono(TargetInfo.class)
                        .block(Duration.ofSeconds(3));
            }
            return commentClient.get()
                    .uri("/api/v1/comments/{id}", targetId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(TargetInfo.class)
                    .block(Duration.ofSeconds(3));
        } catch (WebClientResponseException.NotFound e) {
            throw new ApiException("Target not found", HttpStatus.BAD_REQUEST.value());
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Target resolve failed type={} id={}: {}", type, targetId, e.getMessage());
            throw new ApiException("Target service unavailable", HttpStatus.SERVICE_UNAVAILABLE.value());
        }
    }

    @Override
    public void applyScoreDelta(Vote.TargetType type, UUID targetId, long delta) {
        if (delta == 0) {
            return;
        }
        try {
            if (type == Vote.TargetType.POST) {
                postClient.post()
                        .uri("/api/v1/internal/posts/{id}/score-delta", targetId)
                        .header("X-Internal-Api-Key", internalApiKey)
                        .bodyValue(Map.of("delta", delta))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block(Duration.ofSeconds(3));
            } else {
                commentClient.post()
                        .uri("/api/v1/internal/comments/{id}/score-delta", targetId)
                        .header("X-Internal-Api-Key", internalApiKey)
                        .bodyValue(Map.of("delta", delta))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block(Duration.ofSeconds(3));
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Score delta failed type={} id={} delta={}: {}", type, targetId, delta, e.getMessage());
            throw new ApiException("Failed to update content score", HttpStatus.SERVICE_UNAVAILABLE.value());
        }
    }

    @Override
    public void applyKarmaDelta(UUID authorId, long delta) {
        if (authorId == null || delta == 0) {
            return;
        }
        try {
            authClient.post()
                    .uri("/api/v1/internal/users/{id}/karma-delta", authorId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .bodyValue(Map.of("delta", delta))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block(Duration.ofSeconds(3));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Karma delta failed authorId={} delta={}: {}", authorId, delta, e.getMessage());
            throw new ApiException("Failed to update author karma", HttpStatus.SERVICE_UNAVAILABLE.value());
        }
    }

    private static class TargetInfo {
        public UUID authorId;
        public String title;
        public String content;
    }
}
