package com.threadly.auth.rbac;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Central role → permission matrix for Threadly.
 */
public final class RbacCatalog {

    private RbacCatalog() {
    }

    public static Set<Permission> permissionsForRole(AppRole role) {
        return switch (role) {
            case ROLE_USER -> EnumSet.of(
                    Permission.CONTENT_CREATE,
                    Permission.CONTENT_DELETE_OWN,
                    Permission.COMMUNITY_CREATE,
                    Permission.VOTE_CAST
            );
            case ROLE_MODERATOR -> {
                EnumSet<Permission> set = EnumSet.copyOf(permissionsForRole(AppRole.ROLE_USER));
                set.add(Permission.CONTENT_DELETE_ANY);
                set.add(Permission.COMMUNITY_DELETE);
                yield set;
            }
            case ROLE_ADMIN -> EnumSet.allOf(Permission.class);
        };
    }

    public static Set<String> permissionNamesForRoles(Collection<String> roleNames) {
        Set<String> out = new LinkedHashSet<>();
        for (String name : roleNames) {
            AppRole.fromName(name).ifPresent(role ->
                    permissionsForRole(role).forEach(p -> out.add(p.name())));
        }
        // Unknown custom DB roles still get base member capabilities if named oddly — no-op otherwise.
        return out;
    }

    public static Set<String> permissionNamesForAppRoles(Collection<AppRole> roles) {
        return permissionNamesForRoles(roles.stream().map(Enum::name).collect(Collectors.toSet()));
    }
}
