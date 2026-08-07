package com.threadly.auth.security;

import com.threadly.auth.config.AppSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window rate limiter (per IP). In-memory is fine for single instance;
 * swap to Redis-backed implementation behind the same filter interface if you scale out horizontally.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final AppSecurityProperties properties;

    public RateLimitFilter(AppSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var rate = properties.getRateLimit();
        if (!rate.isEnabled() || request.getRequestURI().startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }
        boolean authPath = request.getRequestURI().contains("/auth/login")
                || request.getRequestURI().contains("/auth/signup")
                || request.getRequestURI().contains("/auth/forgot-password")
                || request.getRequestURI().contains("/auth/reset-password");
        int limit = authPath ? rate.getAuthRequestsPerMinute() : rate.getRequestsPerMinute();
        String ip = clientIp(request);
        String key = ip + (authPath ? "|auth" : "|api");
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.start >= 60_000L) {
                return new Window(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });
        int used = window.count.get();
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - used)));
        if (used > limit) {
            log.warn("Rate limit exceeded ip={} path={} used={}/{}", ip, request.getRequestURI(), used, limit);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private static final class Window {
        private final long start;
        private final AtomicInteger count;

        private Window(long start, AtomicInteger count) {
            this.start = start;
            this.count = count;
        }
    }
}
