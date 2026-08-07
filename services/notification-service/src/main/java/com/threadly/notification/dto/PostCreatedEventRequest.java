package com.threadly.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PostCreatedEventRequest {
    @NotNull
    private Long authorId;
    @NotBlank
    private String authorUsername;
    @NotNull
    private Long postId;
    @NotBlank
    private String postTitle;
    private String communitySlug;
}
