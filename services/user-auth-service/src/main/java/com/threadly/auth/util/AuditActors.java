package com.threadly.auth.util;

import java.util.UUID;

public final class AuditActors {
    private AuditActors() {}

    public static String resolve(String headerUsername, UUID userId) {
        if (headerUsername != null && !headerUsername.isBlank()) {
            return headerUsername.trim().toLowerCase();
        }
        if (userId != null) {
            return "user-" + userId;
        }
        return "system";
    }
}
