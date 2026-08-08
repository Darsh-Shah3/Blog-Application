package com.threadly.post.util;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

/** Formats UUID collections for PostgreSQL uuid[] parameters via string cast. */
public final class PgUuidArray {

    private PgUuidArray() {
    }

    public static String of(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return "{}";
        }
        return ids.stream()
                .filter(id -> id != null)
                .map(UUID::toString)
                .collect(Collectors.joining(",", "{", "}"));
    }
}
