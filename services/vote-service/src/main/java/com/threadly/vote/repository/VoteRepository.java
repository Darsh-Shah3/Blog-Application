package com.threadly.vote.repository;

import com.threadly.vote.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    Optional<Vote> findByUserIdAndTargetTypeAndTargetId(Long userId, Vote.TargetType type, Long targetId);
}
