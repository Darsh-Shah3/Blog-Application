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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteServiceImplTest {

    private static final UUID VOTER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID AUTHOR = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID POST = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock VoteRepository voteRepository;
    @Mock ScoringPort scoringPort;
    @InjectMocks VoteServiceImpl voteService;

    @Test
    void castNewUpvotePropagatesScoreAndKarma() {
        CastVoteRequest req = new CastVoteRequest();
        req.setTargetType(Vote.TargetType.POST);
        req.setTargetId(POST);
        req.setValue((short) 1);

        when(scoringPort.resolveAuthorId(Vote.TargetType.POST, POST)).thenReturn(AUTHOR);
        when(scoringPort.resolveTargetName(Vote.TargetType.POST, POST)).thenReturn("Hello");
        when(voteRepository.findByUserIdAndTargetTypeAndTargetId(VOTER, Vote.TargetType.POST, POST))
                .thenReturn(Optional.empty());
        when(voteRepository.save(any(Vote.class))).thenAnswer(inv -> {
            Vote v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        var res = voteService.cast(VOTER, "voter", req);

        assertEquals((short) 1, res.getValue());
        assertEquals(1L, res.getScoreDeltaApplied());
        verify(scoringPort).applyScoreDelta(Vote.TargetType.POST, POST, 1L);
        verify(scoringPort).applyKarmaDelta(AUTHOR, 1L);
    }

    @Test
    void cannotVoteOnOwnContent() {
        CastVoteRequest req = new CastVoteRequest();
        req.setTargetType(Vote.TargetType.POST);
        req.setTargetId(POST);
        req.setValue((short) 1);
        when(scoringPort.resolveAuthorId(Vote.TargetType.POST, POST)).thenReturn(VOTER);

        ApiException ex = assertThrows(ApiException.class, () -> voteService.cast(VOTER, "voter", req));
        assertEquals(400, ex.getStatus());
        verify(voteRepository, never()).save(any());
    }
}
