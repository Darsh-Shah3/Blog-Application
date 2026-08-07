package com.threadly.auth.rbac;

import java.util.Arrays;
import java.util.Optional;

/**
 * Platform roles stored as {@code roles.name} and embedded in JWT {@code roles} claim.
 * Spring Security maps {@code ROLE_ADMIN} → {@code hasRole('ADMIN')}.
 */
public enum AppRole {
    ROLE_USER("Member", "Create posts & comments, vote, join communities, manage own content"),
    ROLE_MODERATOR("Moderator", "All member rights plus delete any post or comment"),
    ROLE_ADMIN("Administrator", "Full platform control: users, roles, and all content");

    private final String displayName;
    private final String description;

    AppRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static Optional<AppRole> fromName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(r -> r.name().equalsIgnoreCase(name.trim())).findFirst();
    }
}
