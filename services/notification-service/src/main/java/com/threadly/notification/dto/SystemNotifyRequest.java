package com.threadly.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SystemNotifyRequest {
    @NotNull
    private Long userId;
    @NotBlank
    private String type;
    @NotBlank
    private String title;
    private String body;
    private String linkUrl;
    private String actorUsername;
    private String resourceType;
    private String resourceId;
}
