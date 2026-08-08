package com.threadly.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PostCreatedEventRequest {
    @NotNull
    private UUID authorId;
    @NotBlank
    private String authorUsername;
    @NotNull
    private UUID postId;
    @NotBlank
    private String postTitle;
    private String communitySlug;
    /** Creator-provided summary shown under the title in the notification card. */
    private String summary;
    private String mediaUrl;
}
