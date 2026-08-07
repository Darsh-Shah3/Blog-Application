package com.threadly.vote.util;

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
