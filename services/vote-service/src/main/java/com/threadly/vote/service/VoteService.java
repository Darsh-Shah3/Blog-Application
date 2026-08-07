package com.threadly.vote.service;

import com.threadly.vote.dto.CastVoteRequest;
import com.threadly.vote.dto.VoteResponse;
import com.threadly.vote.entity.Vote;

public interface VoteService {

    VoteResponse cast(Long userId, String actorUsername, CastVoteRequest request);

    VoteResponse myVote(Long userId, Vote.TargetType type, Long targetId);
}
