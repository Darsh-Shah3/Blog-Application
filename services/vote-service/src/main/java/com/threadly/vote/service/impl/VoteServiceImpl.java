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

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VoteServiceImpl implements VoteService {

    private static final Logger log = LoggerFactory.getLogger(VoteServiceImpl.class);

    private final VoteRepository voteRepository;
    private final ScoringPort scoringPort;

    @Override
    @Transactional
    public VoteResponse cast(UUID userId, String actorUsername, CastVoteRequest request) {
        if (request.getValue() != 1 && request.getValue() != -1 && request.getValue() != 0) {
            throw new ApiException("value must be -1, 0, or 1", HttpStatus.BAD_REQUEST.value());
        }

        String by = AuditActors.resolve(actorUsername, userId);
        UUID authorId = scoringPort.resolveAuthorId(request.getTargetType(), request.getTargetId());
        if (authorId != null && authorId.equals(userId) && request.getValue() != 0) {
            throw new ApiException("Cannot vote on your own content", HttpStatus.BAD_REQUEST.value());
        }

        Map<String, Object> voterSnap = Map.of("id", userId.toString(), "username", by);
        String targetName = request.getTargetName() != null && !request.getTargetName().isBlank()
                ? request.getTargetName()
                : scoringPort.resolveTargetName(request.getTargetType(), request.getTargetId());
        Map<String, Object> targetSnap = Map.of(
                "id", request.getTargetId().toString(),
                "type", request.getTargetType().name(),
                "name", targetName == null ? "" : targetName
        );

        var existing = voteRepository.findByUserIdAndTargetTypeAndTargetId(
                userId, request.getTargetType(), request.getTargetId());

        long scoreDelta;
        Vote saved;

        if (request.getValue() == 0) {
            if (existing.isEmpty()) {
                return emptyResponse(userId, request, voterSnap, targetSnap);
            }
            Vote vote = existing.get();
            scoreDelta = -vote.getValue();
            voteRepository.delete(vote);
            propagateSideEffects(request.getTargetType(), request.getTargetId(), authorId, scoreDelta);
            return VoteResponse.builder()
                    .userId(userId)
                    .voter(voterSnap)
                    .targetType(request.getTargetType())
                    .targetId(request.getTargetId())
                    .target(targetSnap)
                    .value((short) 0)
                    .scoreDeltaApplied(scoreDelta)
                    .build();
        }

        if (existing.isPresent()) {
            Vote vote = existing.get();
            if (vote.getValue() == request.getValue()) {
                return toResponse(vote, 0L);
            }
            scoreDelta = request.getValue() - vote.getValue();
            vote.setValue(request.getValue());
            vote.setVoter(voterSnap);
            vote.setTarget(targetSnap);
            vote.setUpdatedBy(by);
            saved = voteRepository.save(vote);
        } else {
            scoreDelta = request.getValue();
            saved = voteRepository.save(Vote.builder()
                    .userId(userId)
                    .voter(voterSnap)
                    .targetType(request.getTargetType())
                    .targetId(request.getTargetId())
                    .target(targetSnap)
                    .value(request.getValue())
                    .createdBy(by)
                    .updatedBy(by)
                    .build());
        }

        if (scoreDelta != 0) {
            propagateSideEffects(request.getTargetType(), request.getTargetId(), authorId, scoreDelta);
        }

        return toResponse(saved, scoreDelta);
    }

    private void propagateSideEffects(Vote.TargetType type, UUID targetId, UUID authorId, long delta) {
        boolean scoreApplied = false;
        try {
            scoringPort.applyScoreDelta(type, targetId, delta);
            scoreApplied = true;
            scoringPort.applyKarmaDelta(authorId, delta);
        } catch (RuntimeException ex) {
            if (scoreApplied) {
                try {
                    scoringPort.applyScoreDelta(type, targetId, -delta);
                } catch (RuntimeException compensateEx) {
                    log.error("Score compensation failed: {}", compensateEx.getMessage());
                }
            }
            if (ex instanceof ApiException api) {
                throw api;
            }
            throw new ApiException("Vote side effects failed; local vote rolled back",
                    HttpStatus.SERVICE_UNAVAILABLE.value());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public VoteResponse myVote(UUID userId, Vote.TargetType type, UUID targetId) {
        return voteRepository.findByUserIdAndTargetTypeAndTargetId(userId, type, targetId)
                .map(v -> toResponse(v, 0L))
                .orElse(VoteResponse.builder()
                        .userId(userId)
                        .targetType(type)
                        .targetId(targetId)
                        .value((short) 0)
                        .scoreDeltaApplied(0L)
                        .build());
    }

    private VoteResponse toResponse(Vote v, long scoreDelta) {
        return VoteResponse.builder()
                .id(v.getId())
                .userId(v.getUserId())
                .voter(v.getVoter())
                .targetType(v.getTargetType())
                .targetId(v.getTargetId())
                .target(v.getTarget())
                .value(v.getValue())
                .scoreDeltaApplied(scoreDelta)
                .build();
    }

    private VoteResponse emptyResponse(UUID userId, CastVoteRequest request,
                                       Map<String, Object> voter, Map<String, Object> target) {
        return VoteResponse.builder()
                .userId(userId)
                .voter(voter)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .target(target)
                .value((short) 0)
                .scoreDeltaApplied(0L)
                .build();
    }
}
