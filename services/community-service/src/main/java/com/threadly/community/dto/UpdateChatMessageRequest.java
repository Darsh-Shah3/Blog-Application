package com.threadly.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateChatMessageRequest {
    @NotBlank
    @Size(max = 2000)
    private String body;
}
