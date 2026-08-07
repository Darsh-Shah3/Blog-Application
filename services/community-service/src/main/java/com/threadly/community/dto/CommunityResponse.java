package com.threadly.community.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CommunityResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private Long creatorId;
    private Long memberCount;
    private Instant createdAt;
    private String createdBy;
    private String updatedBy;
    private Boolean joined;
}
