package com.threadly.media.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class MediaResponse {
    private UUID id;
    private String originalName;
    private String contentType;
    private String kind;
    private Long sizeBytes;
    private UUID uploaderId;
    private String uploaderUsername;
    private Map<String, Object> uploader;
    private String url;
    private Instant createdAt;
    private String createdBy;
    private String updatedBy;
}
