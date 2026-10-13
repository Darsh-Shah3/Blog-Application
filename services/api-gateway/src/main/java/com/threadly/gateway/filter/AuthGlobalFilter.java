package com.threadly.gateway.filter;

import com.threadly.gateway.security.JwtService;
import com.threadly.gateway.security.PublicRoutes;
import com.threadly.gateway.security.TokenBlacklistChecker;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Edge JWT validation for every business API.
 * Only signup/login (and CORS preflight / actuator health) are public.
 * <p>
 * Order matters:
 * RateLimit filter runs earlier (−100); this filter at −90 validates identity after buckets allow the call.
 * Client-supplied {@code X-User-Id} / internal key headers are always stripped so callers cannot
 * spoof identity when talking to the gateway.
 */
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthGlobalFilter.class);

    private final JwtService jwtService;
    private final TokenBlacklistChecker tokenBlacklistChecker;

    @Value("${app.security.require-jwt-for-all-apis:true}")
    private boolean requireJwtForAllApis;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";

        String requestId = request.getHeaders().getFirst("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put("requestId", requestId);
        MDC.put("path", path);
        MDC.put("method", method);

        ServerHttpRequest.Builder mutated = request.mutate()
                .headers(h -> {
                    h.remove("X-User-Id");
                    h.remove("X-User-Roles");
                    h.remove("X-User-Permissions");
                    h.remove("X-Username");
                    h.remove("X-Internal-Api-Key"); // never trust client-supplied internal key via public gateway
                })
                .header("X-Request-Id", requestId);

        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            log.debug("CORS preflight allowed path={}", path);
            return chain.filter(exchange.mutate().request(mutated.build()).build())
                    .doFinally(s -> MDC.clear());
        }

        if (isPublic(request)) {
            log.debug("Public route allowed path={} method={}", path, method);
            return chain.filter(exchange.mutate().request(mutated.build()).build())
                    .doFinally(s -> MDC.clear());
        }

        // Specific resource GETs (e.g. /posts/{id}) require login when list GET is public
        if (!requireJwtForAllApis) {
            // legacy mode already handled inside PublicRoutes when expanded
        }

        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            log.warn("Missing Bearer token path={} client={}", path, clientIp(request));
            return unauthorized(exchange, "Missing or invalid Authorization header. Use: Bearer <jwt>");
        }

        try {
            String token = auth.substring(7).trim();
            if (token.isEmpty()) {
                log.warn("Empty Bearer token path={}", path);
                return unauthorized(exchange, "Empty Bearer token");
            }
            Claims claims = jwtService.parse(token);
            String userId = claims.getSubject();
            List<String> roles = jwtService.roles(claims);
            List<String> permissions = jwtService.permissions(claims);
            String username = claims.get("username", String.class);

            return tokenBlacklistChecker.isRevoked(claims).flatMap(revoked -> {
                if (Boolean.TRUE.equals(revoked)) {
                    log.warn("Revoked JWT rejected userId={} path={}", userId, path);
                    return unauthorized(exchange, "Token has been revoked. Please log in again.");
                }

                MDC.put("userId", userId != null ? userId : "-");
                mutated.header("X-User-Id", userId)
                        .header("X-User-Roles", String.join(",", roles))
                        .header("X-User-Permissions", String.join(",", permissions));
                if (username != null) {
                    mutated.header("X-Username", username);
                }

                log.info("JWT authenticated userId={} username={} roles={} path={} method={}",
                        userId, username, roles, path, method);

                return chain.filter(exchange.mutate().request(mutated.build()).build())
                        .doFinally(s -> MDC.clear());
            });
        } catch (Exception ex) {
            log.warn("JWT validation failed path={} reason={}", path, ex.getMessage());
            return unauthorized(exchange, "Invalid or expired JWT token");
        }
    }

    private boolean isPublic(org.springframework.http.server.reactive.ServerHttpRequest request) {
        return PublicRoutes.isPublic(request);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set("WWW-Authenticate", "Bearer");
        String body = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + escape(message) + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)))
                .doFinally(s -> MDC.clear());
    }

    private String escape(String s) {
        return s.replace("\"", "'");
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
        return -90; // after rate limit (-100)
    }
}
