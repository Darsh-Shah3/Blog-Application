package com.threadly.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class CommunityChatEventRequest {
    @NotNull
    private UUID communityId;
    @NotBlank
    private String communityName;
    @NotBlank
    private String communitySlug;
    @NotNull
    private UUID authorId;
    private String authorUsername;
    @NotNull
    private UUID messageId;
    private String bodyPreview;
    /** Members to notify (excluding author preferred — filtered again server-side). */
    private List<UUID> recipientUserIds = new ArrayList<>();
}
