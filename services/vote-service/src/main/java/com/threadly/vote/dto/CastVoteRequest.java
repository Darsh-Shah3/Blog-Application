package com.threadly.vote.dto;

import com.threadly.vote.entity.Vote;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CastVoteRequest {
    @NotNull
    private Vote.TargetType targetType;
    @NotNull
    private UUID targetId;
    /** Optional display name (post title or comment preview). */
    private String targetName;
    @NotNull
    @Min(-1)
    @Max(1)
    private Short value;
}
