package com.threadly.vote.dto;

import com.threadly.vote.entity.Vote;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class VoteResponse {
    private UUID id;
    private UUID userId;
    private Map<String, Object> voter;
    private Vote.TargetType targetType;
    private UUID targetId;
    private Map<String, Object> target;
    private Short value;
    private Long scoreDeltaApplied;
}
