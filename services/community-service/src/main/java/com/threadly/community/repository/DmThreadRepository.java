package com.threadly.community.repository;

import com.threadly.community.entity.DmThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DmThreadRepository extends JpaRepository<DmThread, UUID> {

    Optional<DmThread> findByUserLowAndUserHigh(UUID userLow, UUID userHigh);

    @Query("""
            SELECT t FROM DmThread t
            WHERE t.userLow = :userId OR t.userHigh = :userId
            ORDER BY COALESCE(t.lastMessageAt, t.createdAt) DESC
            """)
    List<DmThread> findInboxForUser(@Param("userId") UUID userId);
}
