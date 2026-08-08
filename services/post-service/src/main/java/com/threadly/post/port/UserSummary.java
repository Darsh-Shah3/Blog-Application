package com.threadly.post.port;

import java.util.UUID;

public record UserSummary(UUID id, String username, String displayName) {
}
