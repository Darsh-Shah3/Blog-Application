package com.threadly.notification.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Best-effort audit publish (soft-delete / clear notifications). */
@Component
public class AuditClient {

    private static final Logger log = LoggerFactory.getLogger(AuditClient.class);

    private final WebClient webClient;
    private final String internalApiKey;

    public AuditClient(
            WebClient.Builder builder,
            @Value("${app.services.audit-base-url:http://localhost:8087}") String auditBaseUrl,
            @Value("${app.internal-api-key}") String internalApiKey) {
        this.webClient = builder.baseUrl(auditBaseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    public void record(String action, String resourceType, String resourceId,
                       UUID actorUserId, String summary, String beforeData) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("serviceName", "notification-service");
            body.put("action", action);
            body.put("resourceType", resourceType);
            body.put("resourceId", resourceId);
            body.put("actorUserId", actorUserId);
            body.put("summary", summary);
            body.put("beforeData", beforeData);
            body.put("requestId", MDC.get("requestId"));
            webClient.post()
                    .uri("/api/v1/internal/audit/events")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(2));
        } catch (Exception e) {
            log.warn("Audit publish failed action={} resource={}/{}: {}",
                    action, resourceType, resourceId, e.getMessage());
        }
    }
}
