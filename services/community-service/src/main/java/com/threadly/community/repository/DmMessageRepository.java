package com.threadly.community.repository;

import com.threadly.community.entity.DmMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface DmMessageRepository extends JpaRepository<DmMessage, UUID> {

    Page<DmMessage> findByThreadIdOrderByCreatedAtDesc(UUID threadId, Pageable pageable);

    @Query(
            value = """
                    SELECT * FROM dm_messages m
                    WHERE m.thread_id = :threadId
                      AND NOT (:userId = ANY (COALESCE(m.hidden_for, '{}')))
                    ORDER BY m.created_at DESC
                    """,
            countQuery = """
                    SELECT count(*) FROM dm_messages m
                    WHERE m.thread_id = :threadId
                      AND NOT (:userId = ANY (COALESCE(m.hidden_for, '{}')))
                    """,
            nativeQuery = true
    )
    Page<DmMessage> findVisibleForUser(
            @Param("threadId") UUID threadId,
            @Param("userId") UUID userId,
            Pageable pageable);
}
