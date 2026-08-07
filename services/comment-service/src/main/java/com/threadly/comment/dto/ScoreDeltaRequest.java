package com.threadly.comment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScoreDeltaRequest {
    @NotNull
    private Long delta;
}
