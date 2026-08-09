package com.threadly.community.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class DmThreadResponse {
    UUID id;
    UUID peerUserId;
    String peerUsername;
    String lastMessagePreview;
    Instant lastMessageAt;
    Instant createdAt;
}
