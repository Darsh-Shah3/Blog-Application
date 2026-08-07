package com.threadly.vote.service.impl;

import com.threadly.vote.dto.CastVoteRequest;
import com.threadly.vote.dto.VoteResponse;
import com.threadly.vote.entity.Vote;
import com.threadly.vote.exception.ApiException;
import com.threadly.vote.port.ScoringPort;
import com.threadly.vote.repository.VoteRepository;
import com.threadly.vote.service.VoteService;
import com.threadly.vote.util.AuditActors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vote state machine with local DB transaction + compensatory remote side effects.
 * <p>
 * Flow for a non-zero delta:
 * <ol>
 *   <li>Mutate vote row in this service (still open transaction)</li>
 *   <li>Apply remote score, then karma</li>
 *   <li>If any remote step fails: compensate previous remotes and throw → JPA rolls back vote row</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class VoteServiceImpl implements VoteService {

    private static final Logger log = LoggerFactory.getLogger(VoteServiceImpl.class);

    private final VoteRepository voteRepository;
    private final ScoringPort scoringPort;

    @Override
    @Transactional
    public VoteResponse cast(Long userId, String actorUsername, CastVoteRequest request) {
        if (request.getValue() != 1 && request.getValue() != -1 && request.getValue() != 0) {
            throw new ApiException("value must be -1, 0, or 1", HttpStatus.BAD_REQUEST.value());
        }

        String by = AuditActors.resolve(actorUsername, userId);
        Long authorId = scoringPort.resolveAuthorId(request.getTargetType(), request.getTargetId());
        if (authorId != null && authorId.equals(userId) && request.getValue() != 0) {
            throw new ApiException("Cannot vote on your own content", HttpStatus.BAD_REQUEST.value());
        }

        var existing = voteRepository.findByUserIdAndTargetTypeAndTargetId(
                userId, request.getTargetType(), request.getTargetId());

        long scoreDelta;
        Vote saved;

        if (request.getValue() == 0) {
            if (existing.isEmpty()) {
                return emptyResponse(userId, request);
            }
            Vote vote = existing.get();
            scoreDelta = -vote.getValue();
            voteRepository.delete(vote);
            propagateSideEffects(request.getTargetType(), request.getTargetId(), authorId, scoreDelta);
            log.info("Vote removed userId={} type={} targetId={} delta={} by={}",
                    userId, request.getTargetType(), request.getTargetId(), scoreDelta, by);
            return VoteResponse.builder()
                    .userId(userId)
                    .targetType(request.getTargetType())
                    .targetId(request.getTargetId())
                    .value((short) 0)
                    .scoreDeltaApplied(scoreDelta)
                    .build();
        }

        if (existing.isPresent()) {
            Vote vote = existing.get();
            if (vote.getValue() == request.getValue()) {
                return VoteResponse.builder()
                        .id(vote.getId())
                        .userId(userId)
                        .targetType(vote.getTargetType())
                        .targetId(vote.getTargetId())
                        .value(vote.getValue())
                        .scoreDeltaApplied(0L)
                        .build();
            }
            scoreDelta = request.getValue() - vote.getValue();
            vote.setValue(request.getValue());
            vote.setUpdatedBy(by);
            saved = voteRepository.save(vote);
        } else {
            scoreDelta = request.getValue();
            saved = voteRepository.save(Vote.builder()
                    .userId(userId)
                    .targetType(request.getTargetType())
                    .targetId(request.getTargetId())
                    .value(request.getValue())
                    .createdBy(by)
                    .updatedBy(by)
                    .build());
        }

        if (scoreDelta != 0) {
            propagateSideEffects(request.getTargetType(), request.getTargetId(), authorId, scoreDelta);
        }
        log.info("Vote cast userId={} type={} targetId={} value={} delta={} by={}",
                userId, request.getTargetType(), request.getTargetId(), request.getValue(), scoreDelta, by);

        return VoteResponse.builder()
                .id(saved.getId())
                .userId(userId)
                .targetType(saved.getTargetType())
                .targetId(saved.getTargetId())
                .value(saved.getValue())
                .scoreDeltaApplied(scoreDelta)
                .build();
    }

    /**
     * Applies score then karma. On karma failure, reverses score then rethrows so local TX rolls back.
     */
    private void propagateSideEffects(Vote.TargetType type, Long targetId, Long authorId, long delta) {
        boolean scoreApplied = false;
        try {
            scoringPort.applyScoreDelta(type, targetId, delta);
            scoreApplied = true;
            scoringPort.applyKarmaDelta(authorId, delta);
        } catch (RuntimeException ex) {
            if (scoreApplied) {
                try {
                    scoringPort.applyScoreDelta(type, targetId, -delta);
                    log.warn("Compensated score after karma failure type={} targetId={} reverseDelta={}",
                            type, targetId, -delta);
                } catch (RuntimeException compensateEx) {
                    log.error("Score compensation failed type={} targetId={} — manual repair may be needed: {}",
                            type, targetId, compensateEx.getMessage());
                }
            }
            if (ex instanceof ApiException api) {
                throw api;
            }
            throw new ApiException(
                    "Vote side effects failed; local vote rolled back",
                    HttpStatus.SERVICE_UNAVAILABLE.value());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public VoteResponse myVote(Long userId, Vote.TargetType type, Long targetId) {
        return voteRepository.findByUserIdAndTargetTypeAndTargetId(userId, type, targetId)
                .map(v -> VoteResponse.builder()
                        .id(v.getId())
                        .userId(v.getUserId())
                        .targetType(v.getTargetType())
                        .targetId(v.getTargetId())
                        .value(v.getValue())
                        .scoreDeltaApplied(0L)
                        .build())
                .orElse(VoteResponse.builder()
                        .userId(userId)
                        .targetType(type)
                        .targetId(targetId)
                        .value((short) 0)
                        .scoreDeltaApplied(0L)
                        .build());
    }

    private VoteResponse emptyResponse(Long userId, CastVoteRequest request) {
        return VoteResponse.builder()
                .userId(userId)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .value((short) 0)
                .scoreDeltaApplied(0L)
                .build();
    }
}
