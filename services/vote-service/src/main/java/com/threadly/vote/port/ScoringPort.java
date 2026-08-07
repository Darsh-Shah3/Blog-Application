package com.threadly.vote.port;

import com.threadly.vote.entity.Vote;

/**
 * Fan-out port for score/karma side effects.
 * Vote domain owns the vote row; other services only receive additive deltas.
 */
public interface ScoringPort {

    Long resolveAuthorId(Vote.TargetType type, Long targetId);

    void applyScoreDelta(Vote.TargetType type, Long targetId, long delta);

    void applyKarmaDelta(Long authorId, long delta);
}
