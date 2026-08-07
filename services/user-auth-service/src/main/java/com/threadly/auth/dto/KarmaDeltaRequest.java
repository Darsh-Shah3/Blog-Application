package com.threadly.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KarmaDeltaRequest {
    @NotNull
    private Long delta;
}
