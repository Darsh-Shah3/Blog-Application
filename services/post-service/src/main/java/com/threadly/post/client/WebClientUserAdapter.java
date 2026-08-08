package com.threadly.post.client;

import com.threadly.post.config.RemoteServiceProperties;
import com.threadly.post.port.UserPort;
import com.threadly.post.port.UserSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class WebClientUserAdapter implements UserPort {

    private static final Logger log = LoggerFactory.getLogger(WebClientUserAdapter.class);

    private final WebClient webClient;
    private final String internalApiKey;

    public WebClientUserAdapter(WebClient.Builder builder, RemoteServiceProperties props) {
        this.webClient = builder.baseUrl(props.getAuthBaseUrl()).build();
        this.internalApiKey = props.getInternalApiKey();
    }

    @Override
    public Optional<UserSummary> findById(UUID userId) {
        try {
            UserDto dto = webClient.get()
                    .uri("/api/v1/internal/users/{id}", userId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(UserDto.class)
                    .block(Duration.ofSeconds(3));
            if (dto == null) {
                return Optional.empty();
            }
            return Optional.of(new UserSummary(dto.id, dto.username, dto.displayName));
        } catch (Exception e) {
            log.warn("Auth lookup failed userId={}: {}", userId, e.getMessage());
            return Optional.of(new UserSummary(userId, "user-" + userId, null));
        }
    }

    @Override
    public List<UUID> findFollowingIds(UUID userId) {
        try {
            List<ContactDto> list = webClient.get()
                    .uri("/api/v1/internal/users/{id}/following", userId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ContactDto>>() {})
                    .block(Duration.ofSeconds(3));
            if (list == null || list.isEmpty()) {
                return List.of();
            }
            return list.stream()
                    .filter(c -> c != null && c.id != null)
                    .map(c -> c.id)
                    .toList();
        } catch (Exception e) {
            log.warn("Following lookup failed userId={}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private static class UserDto {
        public UUID id;
        public String username;
        public String displayName;
    }

    private static class ContactDto {
        public UUID id;
        public String username;
    }
}
