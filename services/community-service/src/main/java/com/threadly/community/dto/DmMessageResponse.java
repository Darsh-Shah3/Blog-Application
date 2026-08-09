package com.threadly.community.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Value
@Builder
public class DmMessageResponse {
    UUID id;
    UUID threadId;
    UUID authorId;
    String authorUsername;
    Map<String, Object> author;
    String body;
    Instant createdAt;
}
