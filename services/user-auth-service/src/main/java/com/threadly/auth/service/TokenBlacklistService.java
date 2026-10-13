package com.threadly.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Writes JWT revoke keys that the API gateway (and community STOMP) check.
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    private final ObjectProvider<StringRedisTemplate> redisProvider;

    @Value("${app.token-blacklist.enabled:false}")
    private boolean enabled;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    public TokenBlacklistService(ObjectProvider<StringRedisTemplate> redisProvider) {
        this.redisProvider = redisProvider;
    }

    public void revokeToken(String jti, Duration ttl) {
        if (!enabled || jti == null || jti.isBlank()) {
            return;
        }
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            log.warn("Token blacklist enabled but Redis unavailable; skip jti revoke");
            return;
        }
        try {
            Duration effective = (ttl == null || ttl.isZero() || ttl.isNegative())
                    ? Duration.ofMillis(jwtExpirationMs)
                    : ttl;
            redis.opsForValue().set("bl:jti:" + jti, "1", effective);
            log.info("Blacklisted JWT jti={} ttlSeconds={}", jti, effective.toSeconds());
        } catch (Exception ex) {
            log.error("Failed to blacklist jti={}: {}", jti, ex.toString());
        }
    }

    /** Revokes all tokens for the user issued at or before now (checked via iat). */
    public void revokeAllForUser(UUID userId) {
        if (!enabled || userId == null) {
            return;
        }
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            log.warn("Token blacklist enabled but Redis unavailable; skip user revoke userId={}", userId);
            return;
        }
        try {
            long now = System.currentTimeMillis();
            redis.opsForValue().set(
                    "bl:user:" + userId,
                    String.valueOf(now),
                    Duration.ofMillis(jwtExpirationMs));
            log.info("Revoked all JWTs for userId={} asOf={}", userId, now);
        } catch (Exception ex) {
            log.error("Failed to revoke JWTs for userId={}: {}", userId, ex.toString());
        }
    }
}
