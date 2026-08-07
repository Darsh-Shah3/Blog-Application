package com.threadly.notification.service;

import com.threadly.notification.dto.NotificationResponse;
import com.threadly.notification.dto.PostCreatedEventRequest;
import com.threadly.notification.dto.SystemNotifyRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    void onPostCreated(PostCreatedEventRequest request);

    NotificationResponse notifyUser(SystemNotifyRequest request);

    Page<NotificationResponse> list(Long userId, boolean unreadOnly, String type, String q, Pageable pageable);

    long unreadCount(Long userId);

    NotificationResponse markRead(Long userId, Long notificationId);

    int markAllRead(Long userId);
}
