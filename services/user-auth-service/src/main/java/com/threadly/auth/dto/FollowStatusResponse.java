package com.threadly.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class FollowStatusResponse {
    private UUID userId;
    private String username;
    private boolean following;
    private long followerCount;
    private long followingCount;
}
