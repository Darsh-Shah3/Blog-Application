package com.threadly.notification.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis counters for notification unread totals (Sidebar polls frequently).
 */
@Component
public class UnreadCountCache {

    private static final Logger log = LoggerFactory.getLogger(UnreadCountCache.class);
    private static final Duration TTL = Duration.ofSeconds(30);

    private final ObjectProvider<StringRedisTemplate> redisProvider;

    @Value("${app.cache.redis-enabled:false}")
    private boolean enabled;

    public UnreadCountCache(ObjectProvider<StringRedisTemplate> redisProvider) {
        this.redisProvider = redisProvider;
    }

    public Optional<Long> get(UUID userId, String type) {
        if (!enabled) {
            return Optional.empty();
        }
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return Optional.empty();
        }
        try {
            String raw = redis.opsForValue().get(key(userId, type));
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(Long.parseLong(raw.trim()));
        } catch (Exception ex) {
            log.warn("Unread cache GET failed: {}", ex.toString());
            return Optional.empty();
        }
    }

    public void set(UUID userId, String type, long count) {
        if (!enabled) {
            return;
        }
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.opsForValue().set(key(userId, type), String.valueOf(Math.max(0, count)), TTL);
        } catch (Exception ex) {
            log.warn("Unread cache SET failed: {}", ex.toString());
        }
    }

    public void increment(UUID userId, String type) {
        if (!enabled || userId == null) {
            return;
        }
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            bump(redis, key(userId, null), 1);
            if (type != null && !type.isBlank()) {
                bump(redis, key(userId, type), 1);
            }
        } catch (Exception ex) {
            log.warn("Unread cache INCR failed: {}", ex.toString());
            invalidate(userId, type);
        }
    }

    public void decrement(UUID userId, String type) {
        if (!enabled || userId == null) {
            return;
        }
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            bump(redis, key(userId, null), -1);
            if (type != null && !type.isBlank()) {
                bump(redis, key(userId, type), -1);
            }
        } catch (Exception ex) {
            log.warn("Unread cache DECR failed: {}", ex.toString());
            invalidate(userId, type);
        }
    }

    public void invalidate(UUID userId, String type) {
        if (!enabled || userId == null) {
            return;
        }
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.delete(key(userId, null));
            if (type != null && !type.isBlank()) {
                redis.delete(key(userId, type));
            }
        } catch (Exception ex) {
            log.warn("Unread cache DEL failed: {}", ex.toString());
        }
    }

    public void invalidateAll(UUID userId) {
        invalidate(userId, null);
        // Typed keys are short-TTL; clearing total forces next typed miss to recompute from DB.
    }

    private void bump(StringRedisTemplate redis, String redisKey, long delta) {
        Boolean exists = redis.hasKey(redisKey);
        if (!Boolean.TRUE.equals(exists)) {
            // Miss: let next read recompute from DB rather than inventing a counter.
            return;
        }
        Long value = redis.opsForValue().increment(redisKey, delta);
        if (value != null && value < 0) {
            redis.opsForValue().set(redisKey, "0", TTL);
        } else {
            redis.expire(redisKey, TTL);
        }
    }

    private static String key(UUID userId, String type) {
        if (type == null || type.isBlank()) {
            return "notif:unread:v1:" + userId;
        }
        return "notif:unread:v1:" + userId + ":" + type.trim();
    }
}
