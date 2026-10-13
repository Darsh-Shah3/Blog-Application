package com.threadly.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window rate limiter applied to every gateway request (per client IP).
 * When {@code app.rate-limit.redis-enabled=true}, counters live in Redis so
 * multiple gateway replicas share the same buckets. Otherwise uses in-memory maps
 * (fine for a single local instance).
 */
@Component
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitGlobalFilter.class);
    private static final Duration WINDOW_TTL = Duration.ofMinutes(2);

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private ReactiveStringRedisTemplate redis;

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.rate-limit.redis-enabled:false}")
    private boolean redisEnabled;

    @Value("${app.rate-limit.requests-per-minute:120}")
    private int requestsPerMinute;

    @Value("${app.rate-limit.auth-requests-per-minute:20}")
    private int authRequestsPerMinute;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!enabled) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String ip = clientIp(request);
        boolean authPath = path != null && (path.startsWith("/api/v1/auth/login")
                || path.startsWith("/api/v1/auth/signup")
                || path.startsWith("/api/v1/auth/forgot-password")
                || path.startsWith("/api/v1/auth/reset-password"));
        int limit = authPath ? authRequestsPerMinute : requestsPerMinute;
        String bucket = authPath ? "auth" : "api";
        String key = ip + "|" + bucket;

        Mono<Integer> usedMono = (redisEnabled && redis != null)
                ? incrementRedis(key)
                : Mono.fromCallable(() -> incrementMemory(key));

        return usedMono.flatMap(used -> {
            exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(limit));
            exchange.getResponse().getHeaders().set(
                    "X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - used)));

            if (used > limit) {
                log.warn("Rate limit exceeded ip={} path={} used={} limit={} store={}",
                        ip, path, used, limit, redisEnabled ? "redis" : "memory");
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                exchange.getResponse().getHeaders().set("Retry-After", "60");
                String body = "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Try again later.\"}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                return exchange.getResponse().writeWith(
                        Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
            }

            if (HttpMethod.OPTIONS.equals(request.getMethod())) {
                return chain.filter(exchange);
            }

            log.debug("Rate limit ok ip={} path={} used={}/{} store={}",
                    ip, path, used, limit, redisEnabled ? "redis" : "memory");
            return chain.filter(exchange);
        }).onErrorResume(ex -> {
            // Fail open if Redis is unreachable so the site stays up; log loudly.
            log.error("Rate limit store error; allowing request ip={} path={}: {}",
                    ip, path, ex.toString());
            return chain.filter(exchange);
        });
    }

    private Mono<Integer> incrementRedis(String logicalKey) {
        long window = System.currentTimeMillis() / 60_000L;
        String redisKey = "rl:" + window + ":" + logicalKey;
        return redis.opsForValue().increment(redisKey)
                .flatMap(count -> {
                    if (count != null && count == 1L) {
                        return redis.expire(redisKey, WINDOW_TTL).thenReturn(count.intValue());
                    }
                    return Mono.just(count == null ? 1 : count.intValue());
                });
    }

    private int incrementMemory(String key) {
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStartMs >= 60_000L) {
                return new Window(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });
        return window.count.get();
    }

    private String clientIp(ServerHttpRequest request) {
        String xff = request.getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private static final class Window {
        private final long windowStartMs;
        private final AtomicInteger count;

        private Window(long windowStartMs, AtomicInteger count) {
            this.windowStartMs = windowStartMs;
            this.count = count;
        }
    }
}
