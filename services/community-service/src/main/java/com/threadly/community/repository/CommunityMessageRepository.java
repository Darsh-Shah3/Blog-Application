package com.threadly.community.repository;

import com.threadly.community.entity.CommunityMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CommunityMessageRepository extends JpaRepository<CommunityMessage, UUID> {

    Page<CommunityMessage> findByCommunityIdOrderByCreatedAtDesc(UUID communityId, Pageable pageable);

    @Query(
            value = """
                    SELECT * FROM community_messages m
                    WHERE m.community_id = :communityId
                      AND NOT (:userId = ANY (COALESCE(m.hidden_for, '{}')))
                    ORDER BY m.created_at DESC
                    """,
            countQuery = """
                    SELECT count(*) FROM community_messages m
                    WHERE m.community_id = :communityId
                      AND NOT (:userId = ANY (COALESCE(m.hidden_for, '{}')))
                    """,
            nativeQuery = true
    )
    Page<CommunityMessage> findVisibleForUser(
            @Param("communityId") UUID communityId,
            @Param("userId") UUID userId,
            Pageable pageable);
}
