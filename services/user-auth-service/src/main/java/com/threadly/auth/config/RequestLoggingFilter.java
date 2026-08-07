package com.threadly.auth.config;

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

/**
 * Puts request correlation into MDC so every log line (INFO/WARN/ERROR/DEBUG) shares format:
 * timestamp · level · service · api METHOD path · Class.method · user · requestId
 */
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
        String userId = header(request, "X-User-Id");
        String username = header(request, "X-Username");

        MDC.put("requestId", requestId);
        MDC.put("httpMethod", method);
        MDC.put("api", path);
        if (userId != null) {
            MDC.put("userId", userId);
        }
        if (username != null) {
            MDC.put("username", username);
        }
        response.setHeader("X-Request-Id", requestId);
        try {
            log.info("→ {} {}", method, path);
            filterChain.doFilter(request, response);
            // Re-read in case JWT filter injected identity mid-chain
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
