package com.threadly.comment.client;

import com.threadly.comment.config.RemoteServiceProperties;
import com.threadly.comment.exception.ApiException;
import com.threadly.comment.port.PostPort;
import com.threadly.comment.port.PostSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class WebClientPostAdapter implements PostPort {

    private static final Logger log = LoggerFactory.getLogger(WebClientPostAdapter.class);
    private final WebClient webClient;
    private final String internalApiKey;

    public WebClientPostAdapter(WebClient.Builder builder, RemoteServiceProperties props) {
        this.webClient = builder.baseUrl(props.getPostBaseUrl()).build();
        this.internalApiKey = props.getInternalApiKey();
    }

    @Override
    public Optional<PostSummary> findById(UUID postId) {
        try {
            PostDto dto = webClient.get()
                    .uri("/api/v1/posts/{id}", postId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(PostDto.class)
                    .block(Duration.ofSeconds(3));
            if (dto == null) {
                return Optional.empty();
            }
            return Optional.of(new PostSummary(dto.id, dto.title));
        } catch (WebClientResponseException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            log.error("Post lookup failed id={}: {}", postId, e.getMessage());
            throw new ApiException("Post service unavailable", HttpStatus.SERVICE_UNAVAILABLE.value());
        }
    }

    @Override
    public void ensureExists(UUID postId) {
        if (findById(postId).isEmpty()) {
            throw new ApiException("Post not found", HttpStatus.BAD_REQUEST.value());
        }
    }

    @Override
    public void adjustCommentCount(UUID postId, long delta) {
        try {
            webClient.post()
                    .uri("/api/v1/internal/posts/{id}/comment-count-delta", postId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .bodyValue(Map.of("delta", delta))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block(Duration.ofSeconds(3));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Comment-count adjust failed postId={}: {}", postId, e.getMessage());
            throw new ApiException(
                    "Failed to update post comment count",
                    HttpStatus.SERVICE_UNAVAILABLE.value());
        }
    }

    private static class PostDto {
        public UUID id;
        public String title;
    }
}
