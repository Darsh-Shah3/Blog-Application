package com.threadly.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private UUID creatorId;
    private String creatorUsername;
    private Map<String, Object> creator;
    private Long memberCount;
    private Instant createdAt;
    private String createdBy;
    private String updatedBy;
    private Boolean joined;
}
