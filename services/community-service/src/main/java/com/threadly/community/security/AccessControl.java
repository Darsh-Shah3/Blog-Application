package com.threadly.community.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.stream.Collectors;

public final class AccessControl {

    private AccessControl() {
    }

    public static boolean hasAuthority(String authority) {
        return authorities().contains(authority);
    }

    public static boolean isAdmin() {
        return authorities().contains("ROLE_ADMIN") || authorities().contains("USER_MANAGE");
    }

    public static boolean canDeleteCommunity() {
        return hasAuthority("COMMUNITY_DELETE") || isAdmin();
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
