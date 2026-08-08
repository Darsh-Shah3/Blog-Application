package com.threadly.community.client;

import com.threadly.community.config.RemoteServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final WebClient webClient;
    private final String internalApiKey;

    public NotificationClient(WebClient.Builder builder, RemoteServiceProperties props) {
        this.webClient = builder.baseUrl(props.getNotificationBaseUrl()).build();
        this.internalApiKey = props.getInternalApiKey();
    }

    public void notifyCommunityChat(
            UUID communityId,
            String communityName,
            String communitySlug,
            UUID authorId,
            String authorUsername,
            UUID messageId,
            String bodyPreview,
            List<UUID> recipientUserIds) {
        if (recipientUserIds == null || recipientUserIds.isEmpty()) {
            return;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("communityId", communityId);
            body.put("communityName", communityName);
            body.put("communitySlug", communitySlug);
            body.put("authorId", authorId);
            body.put("authorUsername", authorUsername);
            body.put("messageId", messageId);
            body.put("bodyPreview", bodyPreview);
            body.put("recipientUserIds", recipientUserIds);
            webClient.post()
                    .uri("/api/v1/internal/notifications/community-chat")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(5));
        } catch (Exception e) {
            log.warn("Community chat notification fan-out failed communityId={}: {}",
                    communityId, e.getMessage());
        }
    }
}
