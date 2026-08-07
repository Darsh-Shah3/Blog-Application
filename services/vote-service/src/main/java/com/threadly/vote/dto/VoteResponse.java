package com.threadly.vote.dto;

import com.threadly.vote.entity.Vote;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VoteResponse {
    private Long id;
    private Long userId;
    private Vote.TargetType targetType;
    private Long targetId;
    private Short value;
    private Long scoreDeltaApplied;
}
