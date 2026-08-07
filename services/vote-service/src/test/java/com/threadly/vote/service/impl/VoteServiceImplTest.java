package com.threadly.vote.service.impl;

import com.threadly.vote.dto.CastVoteRequest;
import com.threadly.vote.entity.Vote;
import com.threadly.vote.exception.ApiException;
import com.threadly.vote.port.ScoringPort;
import com.threadly.vote.repository.VoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteServiceImplTest {

    @Mock VoteRepository voteRepository;
    @Mock ScoringPort scoringPort;
    @InjectMocks VoteServiceImpl voteService;

    @Test
    void castNewUpvotePropagatesScoreAndKarma() {
        CastVoteRequest req = new CastVoteRequest();
        req.setTargetType(Vote.TargetType.POST);
        req.setTargetId(5L);
        req.setValue((short) 1);

        when(scoringPort.resolveAuthorId(Vote.TargetType.POST, 5L)).thenReturn(99L);
        when(voteRepository.findByUserIdAndTargetTypeAndTargetId(1L, Vote.TargetType.POST, 5L))
                .thenReturn(Optional.empty());
        when(voteRepository.save(any(Vote.class))).thenAnswer(inv -> {
            Vote v = inv.getArgument(0);
            v.setId(7L);
            return v;
        });

        var res = voteService.cast(1L, "voter", req);

        assertEquals(1, res.getValue());
        assertEquals(1L, res.getScoreDeltaApplied());
        verify(scoringPort).applyScoreDelta(Vote.TargetType.POST, 5L, 1L);
        verify(scoringPort).applyKarmaDelta(99L, 1L);
    }

    @Test
    void whenKarmaFailsScoreIsCompensatedAndVoteNotKeptConceptually() {
        CastVoteRequest req = new CastVoteRequest();
        req.setTargetType(Vote.TargetType.POST);
        req.setTargetId(5L);
        req.setValue((short) 1);

        when(scoringPort.resolveAuthorId(Vote.TargetType.POST, 5L)).thenReturn(99L);
        when(voteRepository.findByUserIdAndTargetTypeAndTargetId(1L, Vote.TargetType.POST, 5L))
                .thenReturn(Optional.empty());
        when(voteRepository.save(any(Vote.class))).thenAnswer(inv -> {
            Vote v = inv.getArgument(0);
            v.setId(7L);
            return v;
        });
        doNothing().when(scoringPort).applyScoreDelta(Vote.TargetType.POST, 5L, 1L);
        doThrow(new ApiException("Failed to update author karma", 503))
                .when(scoringPort).applyKarmaDelta(99L, 1L);

        assertThrows(ApiException.class, () -> voteService.cast(1L, "voter", req));

        verify(scoringPort).applyScoreDelta(Vote.TargetType.POST, 5L, 1L);
        verify(scoringPort).applyScoreDelta(Vote.TargetType.POST, 5L, -1L);
        verify(scoringPort).applyKarmaDelta(99L, 1L);
    }

    @Test
    void cannotVoteOnOwnContent() {
        CastVoteRequest req = new CastVoteRequest();
        req.setTargetType(Vote.TargetType.POST);
        req.setTargetId(5L);
        req.setValue((short) 1);
        when(scoringPort.resolveAuthorId(Vote.TargetType.POST, 5L)).thenReturn(1L);

        ApiException ex = assertThrows(ApiException.class, () -> voteService.cast(1L, "voter", req));
        assertEquals(400, ex.getStatus());
        verify(voteRepository, never()).save(any());
    }
}
