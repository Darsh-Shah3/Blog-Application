package com.threadly.community.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class RedisJsonCache {

    private static final Logger log = LoggerFactory.getLogger(RedisJsonCache.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisJsonCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String raw = redis.opsForValue().get(key);
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(raw, type));
        } catch (Exception ex) {
            log.warn("Redis GET failed key={}: {}", key, ex.toString());
            return Optional.empty();
        }
    }

    public <T> Optional<T> get(String key, TypeReference<T> type) {
        try {
            String raw = redis.opsForValue().get(key);
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(raw, type));
        } catch (Exception ex) {
            log.warn("Redis GET failed key={}: {}", key, ex.toString());
            return Optional.empty();
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            String raw = objectMapper.writeValueAsString(value);
            if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                redis.opsForValue().set(key, raw, ttl);
            } else {
                redis.opsForValue().set(key, raw);
            }
        } catch (Exception ex) {
            log.warn("Redis SET failed key={}: {}", key, ex.toString());
        }
    }

    public void delete(String key) {
        try {
            redis.delete(key);
        } catch (Exception ex) {
            log.warn("Redis DEL failed key={}: {}", key, ex.toString());
        }
    }

    public void deleteByPrefix(String prefix) {
        try {
            Set<String> keys = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions().match(prefix + "*").count(200).build();
            try (Cursor<String> cursor = redis.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }
            if (!keys.isEmpty()) {
                redis.delete(keys);
            }
        } catch (Exception ex) {
            log.warn("Redis SCAN/DEL failed prefix={}: {}", prefix, ex.toString());
        }
    }

    public StringRedisTemplate template() {
        return redis;
    }
}
