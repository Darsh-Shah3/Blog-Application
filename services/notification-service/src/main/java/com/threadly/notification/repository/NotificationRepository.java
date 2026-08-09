package com.threadly.notification.repository;

import com.threadly.notification.entity.Notification;
import com.threadly.notification.repository.query.NotificationQueries;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query(NotificationQueries.SEARCH_FOR_USER)
    Page<Notification> search(
            @Param("userId") UUID userId,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("type") String type,
            @Param("q") String q,
            Pageable pageable);

    long countByUserIdAndReadAtIsNull(UUID userId);

    long countByUserIdAndTypeAndReadAtIsNull(UUID userId, String type);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("""
            update Notification n
            set n.readAt = :now
            where n.userId = :userId
              and n.readAt is null
              and (:type is null or n.type = :type)
              and (:actorUsername is null or lower(n.actorUsername) = lower(:actorUsername))
            """)
    int markReadMatching(
            @Param("userId") UUID userId,
            @Param("type") String type,
            @Param("actorUsername") String actorUsername,
            @Param("now") java.time.Instant now);
}
