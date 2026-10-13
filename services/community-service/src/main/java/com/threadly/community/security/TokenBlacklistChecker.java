package com.threadly.community.security;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * STOMP JWT blacklist check (WebSocket bypasses the API gateway auth filter).
 */
@Component
public class TokenBlacklistChecker {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistChecker.class);

    private final ObjectProvider<StringRedisTemplate> redisProvider;

    @Value("${app.token-blacklist.enabled:false}")
    private boolean enabled;

    public TokenBlacklistChecker(ObjectProvider<StringRedisTemplate> redisProvider) {
        this.redisProvider = redisProvider;
    }

    public boolean isRevoked(Claims claims) {
        if (!enabled || claims == null) {
            return false;
        }
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return false;
        }
        try {
            String jti = claims.getId();
            if (jti != null && !jti.isBlank() && Boolean.TRUE.equals(redis.hasKey("bl:jti:" + jti))) {
                return true;
            }
            String userId = claims.getSubject();
            Date iat = claims.getIssuedAt();
            if (userId == null || userId.isBlank() || iat == null) {
                return false;
            }
            String raw = redis.opsForValue().get("bl:user:" + userId);
            if (raw == null || raw.isBlank()) {
                return false;
            }
            long revokedAt = Long.parseLong(raw.trim());
            return iat.getTime() <= revokedAt;
        } catch (Exception ex) {
            log.error("STOMP blacklist check failed; allowing token: {}", ex.toString());
            return false;
        }
    }
}
