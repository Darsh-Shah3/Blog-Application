package com.threadly.post.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Role / permission checks from the JWT-backed security context. */
public final class AccessControl {

    private AccessControl() {
    }

    public static boolean hasAuthority(String authority) {
        return authorities().contains(authority);
    }

    public static boolean hasAnyRole(String... roles) {
        Set<String> auths = authorities();
        return Arrays.stream(roles).anyMatch(auths::contains);
    }

    /** Moderators and admins may remove any post/comment. */
    public static boolean canModerateContent() {
        return hasAuthority("CONTENT_DELETE_ANY")
                || hasAnyRole("ROLE_MODERATOR", "ROLE_ADMIN");
    }

    public static boolean canDeleteCommunity() {
        return hasAuthority("COMMUNITY_DELETE") || hasAnyRole("ROLE_ADMIN");
    }

    private static Set<String> authorities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return Set.of();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
