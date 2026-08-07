package com.threadly.post.client;

import com.threadly.post.config.RemoteServiceProperties;
import com.threadly.post.port.AuditPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class WebClientAuditAdapter implements AuditPort {

    private static final Logger log = LoggerFactory.getLogger(WebClientAuditAdapter.class);
    private final WebClient webClient;
    private final String internalApiKey;

    public WebClientAuditAdapter(WebClient.Builder builder, RemoteServiceProperties props) {
        this.webClient = builder.baseUrl(props.getAuditBaseUrl()).build();
        this.internalApiKey = props.getInternalApiKey();
    }

    @Override
    public void record(String action, String resourceType, String resourceId,
                       Long actorUserId, String actorUsername, String summary) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("serviceName", "post-service");
            body.put("action", action);
            body.put("resourceType", resourceType);
            body.put("resourceId", resourceId);
            body.put("actorUserId", actorUserId);
            body.put("actorUsername", actorUsername);
            body.put("summary", summary);
            body.put("requestId", MDC.get("requestId"));
            webClient.post()
                    .uri("/api/v1/internal/audit/events")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(2));
        } catch (Exception e) {
            log.warn("Audit publish failed action={} resource={}/{}: {}", action, resourceType, resourceId, e.getMessage());
        }
    }
}
