package com.threadly.post.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScoreDeltaRequest {
    @NotNull
    private Long delta;
}
