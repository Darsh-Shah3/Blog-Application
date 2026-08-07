package com.threadly.comment.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        String method = request.getMethod();
        String path = request.getRequestURI();
        MDC.put("requestId", requestId);
        MDC.put("httpMethod", method);
        MDC.put("api", path);
        putIfPresent("userId", header(request, "X-User-Id"));
        putIfPresent("username", header(request, "X-Username"));
        response.setHeader("X-Request-Id", requestId);
        try {
            log.info("→ {} {}", method, path);
            filterChain.doFilter(request, response);
            putIfPresent("userId", header(request, "X-User-Id"));
            putIfPresent("username", header(request, "X-Username"));
            long ms = System.currentTimeMillis() - start;
            int status = response.getStatus();
            if (status >= 500) {
                log.error("← {} {} status={} durationMs={}", method, path, status, ms);
            } else if (status >= 400) {
                log.warn("← {} {} status={} durationMs={}", method, path, status, ms);
            } else {
                log.info("← {} {} status={} durationMs={}", method, path, status, ms);
            }
        } finally {
            MDC.clear();
        }
    }

    private static String header(HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private static void putIfPresent(String key, String value) {
        if (value != null) {
            MDC.put(key, value);
        }
    }
}
