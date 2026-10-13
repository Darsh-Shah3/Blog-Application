package com.threadly.gateway.security;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * Public unauthenticated routes. List/browse GETs for posts & communities are public;
 * mutations always require JWT.
 */
public final class PublicRoutes {

    private PublicRoutes() {
    }

    public static boolean isPublic(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        if (path == null) {
            return false;
        }
        HttpMethod method = request.getMethod();
        if (HttpMethod.OPTIONS.equals(method)) {
            return true;
        }

        if (path.equals("/api/v1/auth/signup")
                || path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/forgot-password")
                || path.equals("/api/v1/auth/reset-password")
                || path.equals("/api/v1/auth/captcha-config")
                // Social login start + callback — guests must reach these without a JWT
                || path.startsWith("/api/v1/auth/oauth/")) {
            return true;
        }
        // WebSocket upgrade for STOMP community chat (JWT on STOMP CONNECT)
        if (path.startsWith("/ws/") || path.equals("/ws")) {
            return true;
        }
        // Prod-safe: only health is unauthenticated at the edge (prometheus scraped on Docker network)
        if (path.equals("/actuator/health") || path.startsWith("/actuator/health/")) {
            return true;
        }

        if (HttpMethod.GET.equals(method)) {
            // List / browse (get-all) style
            if (path.equals("/api/v1/posts") || path.equals("/api/v1/communities")
                    || path.equals("/api/v1/communities/discover")) {
                return true;
            }
            // Single post + related for public reading
            if (path.matches("^/api/v1/posts/[^/]+$")
                    || path.matches("^/api/v1/posts/[^/]+/related$")) {
                return true;
            }
            // Comment threads (read-only)
            if (path.matches("^/api/v1/comments/threads/[^/]+$")) {
                return true;
            }
            // Allow reading community pages by slug for public browsing
            if (path.startsWith("/api/v1/communities/by-slug/")) {
                return true;
            }
            // Community chat history (send still requires JWT)
            if (path.matches("^/api/v1/communities/[^/]+/messages$")) {
                return true;
            }
            // Public media bytes
            if (path.matches("/api/v1/media/[^/]+/content")
                    || path.matches("/api/v1/media/[^/]+")) {
                return true;
            }
            // Optional public profile “card” for known usernames in feeds
            if (path.startsWith("/api/v1/profiles/") && !path.contains("/follow")) {
                return true;
            }
        }
        return false;
    }
}
