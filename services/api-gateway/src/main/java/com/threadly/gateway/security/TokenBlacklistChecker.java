package com.threadly.gateway.security;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Date;

/**
 * Checks Redis JWT blacklist keys written by user-auth-service on logout / account revoke.
 * Keys: {@code bl:jti:{jti}} and {@code bl:user:{userId}} (epoch ms of revoke-all).
 */
@Component
public class TokenBlacklistChecker {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistChecker.class);

    @Autowired(required = false)
    private ReactiveStringRedisTemplate redis;

    @Value("${app.token-blacklist.enabled:false}")
    private boolean enabled;

    /**
     * @return true if the token is revoked and must be rejected
     */
    public Mono<Boolean> isRevoked(Claims claims) {
        if (!enabled || redis == null || claims == null) {
            return Mono.just(false);
        }
        String jti = claims.getId();
        String userId = claims.getSubject();
        Date iat = claims.getIssuedAt();

        Mono<Boolean> jtiHit = (jti == null || jti.isBlank())
                ? Mono.just(false)
                : redis.hasKey("bl:jti:" + jti).defaultIfEmpty(false);

        Mono<Boolean> userHit;
        if (userId == null || userId.isBlank() || iat == null) {
            userHit = Mono.just(false);
        } else {
            userHit = redis.opsForValue().get("bl:user:" + userId)
                    .map(raw -> {
                        try {
                            long revokedAt = Long.parseLong(raw.trim());
                            return iat.getTime() <= revokedAt;
                        } catch (NumberFormatException e) {
                            return true;
                        }
                    })
                    .defaultIfEmpty(false);
        }

        return Mono.zip(jtiHit, userHit)
                .map(t -> Boolean.TRUE.equals(t.getT1()) || Boolean.TRUE.equals(t.getT2()))
                .onErrorResume(ex -> {
                    log.error("Token blacklist check failed; allowing token: {}", ex.toString());
                    return Mono.just(false);
                });
    }
}
