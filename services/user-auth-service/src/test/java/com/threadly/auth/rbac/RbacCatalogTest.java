package com.threadly.auth.rbac;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RbacCatalogTest {

    @Test
    void userPermissionsDoNotIncludeAdminRights() {
        Set<String> perms = RbacCatalog.permissionNamesForRoles(Set.of("ROLE_USER"));
        assertTrue(perms.contains("CONTENT_CREATE"));
        assertTrue(perms.contains("CONTENT_DELETE_OWN"));
        assertFalse(perms.contains("CONTENT_DELETE_ANY"));
        assertFalse(perms.contains("ROLE_ASSIGN"));
    }

    @Test
    void moderatorCanDeleteAnyContent() {
        Set<String> perms = RbacCatalog.permissionNamesForRoles(Set.of("ROLE_MODERATOR"));
        assertTrue(perms.contains("CONTENT_DELETE_ANY"));
        assertTrue(perms.contains("CONTENT_CREATE"));
        assertFalse(perms.contains("ROLE_ASSIGN"));
    }

    @Test
    void adminHasAllPermissions() {
        Set<String> perms = RbacCatalog.permissionNamesForRoles(Set.of("ROLE_ADMIN"));
        assertTrue(perms.containsAll(Set.of(
                "CONTENT_CREATE", "CONTENT_DELETE_OWN", "CONTENT_DELETE_ANY",
                "COMMUNITY_CREATE", "COMMUNITY_DELETE", "VOTE_CAST",
                "USER_MANAGE", "ROLE_ASSIGN"
        )));
    }

    @Test
    void combiningRolesUnionsPermissions() {
        Set<String> perms = RbacCatalog.permissionNamesForRoles(Set.of("ROLE_USER", "ROLE_ADMIN"));
        assertTrue(perms.contains("ROLE_ASSIGN"));
        assertTrue(perms.contains("CONTENT_CREATE"));
    }
}
