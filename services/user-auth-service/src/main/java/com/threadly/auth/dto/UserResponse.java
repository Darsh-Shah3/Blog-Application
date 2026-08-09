package com.threadly.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String username;
    private String email;
    private String displayName;
    private String bio;
    /** media-service UUID for the user's display picture, if set. */
    private UUID avatarMediaId;
    /** Relative content URL when avatarMediaId is set (served by media-service via gateway). */
    private String avatarUrl;
    private Long karma;
    private Set<String> roles;
    private Set<String> permissions;
    private Instant createdAt;
    private String createdBy;
    private String updatedBy;
    /** Soft-deleted account — posts remain; show as deactivated in UI. */
    private Boolean deleted;
}
