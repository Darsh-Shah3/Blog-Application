package com.threadly.notification.repository;

import com.threadly.notification.entity.Notification;
import com.threadly.notification.repository.query.NotificationQueries;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query(NotificationQueries.SEARCH_FOR_USER)
    Page<Notification> search(
            @Param("userId") Long userId,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("type") String type,
            @Param("q") String q,
            Pageable pageable);

    long countByUserIdAndReadAtIsNull(Long userId);
}
