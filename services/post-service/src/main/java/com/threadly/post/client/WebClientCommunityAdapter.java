package com.threadly.post.client;

import com.threadly.post.config.RemoteServiceProperties;
import com.threadly.post.port.CommunityPort;
import com.threadly.post.port.CommunitySummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class WebClientCommunityAdapter implements CommunityPort {

    private static final Logger log = LoggerFactory.getLogger(WebClientCommunityAdapter.class);

    private final WebClient webClient;
    private final String internalApiKey;

    public WebClientCommunityAdapter(WebClient.Builder builder, RemoteServiceProperties props) {
        this.webClient = builder.baseUrl(props.getCommunityBaseUrl()).build();
        this.internalApiKey = props.getInternalApiKey();
    }

    @Override
    public Optional<CommunitySummary> findById(UUID communityId) {
        try {
            CommunityDto dto = webClient.get()
                    .uri("/api/v1/communities/{id}", communityId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(CommunityDto.class)
                    .block(Duration.ofSeconds(3));
            if (dto == null) {
                return Optional.empty();
            }
            return Optional.of(new CommunitySummary(dto.id, dto.name, dto.slug));
        } catch (WebClientResponseException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            log.error("Community service call failed id={}: {}", communityId, e.getMessage());
            throw e;
        }
    }

    @Override
    public List<UUID> findJoinedCommunityIds(UUID userId) {
        try {
            List<UUID> ids = webClient.get()
                    .uri("/api/v1/communities/memberships/user/{userId}", userId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<UUID>>() {})
                    .block(Duration.ofSeconds(3));
            return ids == null ? Collections.emptyList() : ids;
        } catch (Exception e) {
            log.warn("Failed joined communities userId={}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private static class CommunityDto {
        public UUID id;
        public String name;
        public String slug;
    }
}
