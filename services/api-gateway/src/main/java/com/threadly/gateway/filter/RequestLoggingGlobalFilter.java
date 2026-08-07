package com.threadly.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Access logging for every request through the gateway (method, path, status, duration).
 * MDC keys align with service logback pattern: httpMethod, api, userId, username, requestId.
 */
@Component
public class RequestLoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingGlobalFilter.class);
    private static final String START_ATTR = "threadly.requestStartMs";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getAttributes().put(START_ATTR, System.currentTimeMillis());
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String path = request.getURI().getPath();
        String requestId = request.getHeaders().getFirst("X-Request-Id");
        String userId = request.getHeaders().getFirst("X-User-Id");
        String username = request.getHeaders().getFirst("X-Username");

        MDC.put("httpMethod", method);
        MDC.put("api", path);
        if (requestId != null) {
            MDC.put("requestId", requestId);
        }
        if (userId != null) {
            MDC.put("userId", userId);
        }
        if (username != null) {
            MDC.put("username", username);
        }

        log.info("→ {} {} remote={}", method, path,
                request.getRemoteAddress() != null ? request.getRemoteAddress() : "-");

        return chain.filter(exchange)
                .doOnSuccess(v -> logCompletion(exchange, method, path, requestId))
                .doOnError(e -> logCompletion(exchange, method, path, requestId))
                .doFinally(s -> MDC.clear());
    }

    private void logCompletion(ServerWebExchange exchange, String method, String path, String requestId) {
        Long start = exchange.getAttribute(START_ATTR);
        long duration = start == null ? -1 : System.currentTimeMillis() - start;
        int status = exchange.getResponse().getStatusCode() != null
                ? exchange.getResponse().getStatusCode().value()
                : 0;
        String uid = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        String uname = exchange.getRequest().getHeaders().getFirst("X-Username");
        if (uid != null) {
            MDC.put("userId", uid);
        }
        if (uname != null) {
            MDC.put("username", uname);
        }
        if (requestId != null) {
            MDC.put("requestId", requestId);
        }
        if (status >= 500) {
            log.error("← {} {} status={} durationMs={}", method, path, status, duration);
        } else if (status >= 400) {
            log.warn("← {} {} status={} durationMs={}", method, path, status, duration);
        } else {
            log.info("← {} {} status={} durationMs={}", method, path, status, duration);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
