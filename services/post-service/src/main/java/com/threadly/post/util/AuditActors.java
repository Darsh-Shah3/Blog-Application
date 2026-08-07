package com.threadly.post.util;

/**
 * Resolves the human-readable actor for created_by / updated_by columns.
 */
public final class AuditActors {
    private AuditActors() {}

    public static String resolve(String headerUsername, Long userId) {
        if (headerUsername != null && !headerUsername.isBlank()) {
            return headerUsername.trim().toLowerCase();
        }
        if (userId != null) {
            return "user-" + userId;
        }
        return "system";
    }
}
