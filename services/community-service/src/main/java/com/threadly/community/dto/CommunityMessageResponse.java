package com.threadly.community.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class CommunityMessageResponse {
    private UUID id;
    private UUID communityId;
    private UUID authorId;
    private String authorUsername;
    private Map<String, Object> author;
    private String body;
    private Instant createdAt;
    private Instant editedAt;
    private boolean deletedForEveryone;
}
