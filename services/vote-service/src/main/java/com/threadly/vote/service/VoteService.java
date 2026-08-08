package com.threadly.vote.service;

import com.threadly.vote.dto.CastVoteRequest;
import com.threadly.vote.dto.VoteResponse;
import com.threadly.vote.entity.Vote;

import java.util.UUID;

public interface VoteService {
    VoteResponse cast(UUID userId, String actorUsername, CastVoteRequest request);

    VoteResponse myVote(UUID userId, Vote.TargetType type, UUID targetId);
}
