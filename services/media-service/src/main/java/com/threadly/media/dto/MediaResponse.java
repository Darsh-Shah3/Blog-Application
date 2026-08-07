package com.threadly.media.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class MediaResponse {
    private Long id;
    private String originalName;
    private String contentType;
    /** IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, OTHER */
    private String kind;
    private Long sizeBytes;
    private Long uploaderId;
    private String url;
    private Instant createdAt;
    private String createdBy;
    private String updatedBy;
}
