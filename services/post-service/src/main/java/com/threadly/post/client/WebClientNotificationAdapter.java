package com.threadly.post.client;

import com.threadly.post.config.RemoteServiceProperties;
import com.threadly.post.port.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class WebClientNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(WebClientNotificationAdapter.class);
    private final WebClient webClient;
    private final String internalApiKey;

    public WebClientNotificationAdapter(WebClient.Builder builder, RemoteServiceProperties props) {
        this.webClient = builder.baseUrl(props.getNotificationBaseUrl()).build();
        this.internalApiKey = props.getInternalApiKey();
    }

    @Override
    public void notifyPostCreated(UUID authorId, String authorUsername, UUID postId, String title,
                                  String communitySlug, String summary, String mediaUrl) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("authorId", authorId);
            body.put("authorUsername", authorUsername);
            body.put("postId", postId);
            body.put("postTitle", title);
            body.put("communitySlug", communitySlug);
            body.put("summary", summary);
            body.put("mediaUrl", mediaUrl);
            webClient.post()
                    .uri("/api/v1/internal/notifications/post-created")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(5));
        } catch (Exception e) {
            log.warn("Notification fan-out failed postId={}: {}", postId, e.getMessage());
        }
    }
}
