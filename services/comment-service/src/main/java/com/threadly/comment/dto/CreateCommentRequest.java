package com.threadly.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateCommentRequest {
    @NotNull
    private UUID postId;
    private UUID parentId;
    @NotBlank
    private String content;
}
