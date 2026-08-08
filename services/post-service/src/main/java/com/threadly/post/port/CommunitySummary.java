package com.threadly.post.port;

import java.util.UUID;

public record CommunitySummary(UUID id, String name, String slug) {
}
