package com.threadly.vote.repository;

import com.threadly.vote.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, UUID> {
    Optional<Vote> findByUserIdAndTargetTypeAndTargetId(UUID userId, Vote.TargetType type, UUID targetId);
}
