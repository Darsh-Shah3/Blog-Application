package com.threadly.community.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class CreateChatMessageRequest {
    @NotBlank
    @Size(max = 2000)
    private String body;
}
