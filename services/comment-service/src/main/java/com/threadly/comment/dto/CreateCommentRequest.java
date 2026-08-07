package com.threadly.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCommentRequest {
    @NotNull
    private Long postId;
    private Long parentId;
    @NotBlank
    private String content;
}
