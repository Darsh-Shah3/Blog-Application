package com.threadly.notification.service;

import com.threadly.notification.dto.CommunityChatEventRequest;
import com.threadly.notification.dto.NotificationResponse;
import com.threadly.notification.dto.PostCreatedEventRequest;
import com.threadly.notification.dto.SystemNotifyRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {
    void onPostCreated(PostCreatedEventRequest request);

    void onCommunityChatMessage(CommunityChatEventRequest request);

    NotificationResponse notifyUser(SystemNotifyRequest request);

    Page<NotificationResponse> list(UUID userId, boolean unreadOnly, String type, String q, Pageable pageable);

    long unreadCount(UUID userId);

    NotificationResponse markRead(UUID userId, UUID notificationId);

    int markAllRead(UUID userId);
}
