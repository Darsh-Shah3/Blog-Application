package com.threadly.comment.client;

import com.threadly.comment.config.RemoteServiceProperties;
import com.threadly.comment.port.UserPort;
import com.threadly.comment.port.UserSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Optional;

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
    public Optional<UserSummary> findById(Long userId) {
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
            return Optional.of(new UserSummary(dto.id, dto.username));
        } catch (Exception e) {
            log.warn("Auth lookup failed userId={}: {}", userId, e.getMessage());
            return Optional.of(new UserSummary(userId, "user-" + userId));
        }
    }

    private static class UserDto {
        public Long id;
        public String username;
    }
}
