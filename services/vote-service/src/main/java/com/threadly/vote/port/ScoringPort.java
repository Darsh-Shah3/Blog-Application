package com.threadly.vote.port;

import com.threadly.vote.entity.Vote;

import java.util.UUID;

public interface ScoringPort {

    UUID resolveAuthorId(Vote.TargetType type, UUID targetId);

    /** Optional title/preview for denormalized vote.target JSON. */
    String resolveTargetName(Vote.TargetType type, UUID targetId);

    void applyScoreDelta(Vote.TargetType type, UUID targetId, long delta);

    void applyKarmaDelta(UUID authorId, long delta);
}
