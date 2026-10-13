package com.threadly.community.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CommunityEditorResponse {
    private UUID userId;
    private String username;
}
