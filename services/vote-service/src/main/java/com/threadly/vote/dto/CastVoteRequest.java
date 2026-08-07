package com.threadly.vote.dto;

import com.threadly.vote.entity.Vote;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CastVoteRequest {
    @NotNull
    private Vote.TargetType targetType;
    @NotNull
    private Long targetId;
    @NotNull
    @Min(-1)
    @Max(1)
    private Short value;
}
