package com.threadly.post.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentCountDeltaRequest {
    @NotNull
    private Long delta;
}
