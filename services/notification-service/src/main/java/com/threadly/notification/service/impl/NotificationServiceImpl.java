package com.threadly.notification.service.impl;

import com.threadly.notification.client.AuthClient;
import com.threadly.notification.dto.NotificationResponse;
import com.threadly.notification.dto.PostCreatedEventRequest;
import com.threadly.notification.dto.SystemNotifyRequest;
import com.threadly.notification.entity.Notification;
import com.threadly.notification.exception.ApiException;
import com.threadly.notification.repository.NotificationRepository;
import com.threadly.notification.service.NotificationService;
import com.threadly.notification.service.OutboundMailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository repository;
    private final AuthClient authClient;
    private final OutboundMailService mailService;

    @Override
    @Transactional
    public void onPostCreated(PostCreatedEventRequest request) {
        List<AuthClient.FollowerContact> followers = authClient.listFollowers(request.getAuthorId());
        if (followers.isEmpty()) {
            log.info("No followers to notify for postId={} author={}", request.getPostId(), request.getAuthorUsername());
            return;
        }
        String link = "/post/" + request.getPostId();
        String title = "@" + request.getAuthorUsername() + " published a new post";
        String body = "\"" + truncate(request.getPostTitle(), 120) + "\"";
        if (request.getCommunitySlug() != null && !request.getCommunitySlug().isBlank()) {
            body += " in r/" + request.getCommunitySlug();
        }

        for (AuthClient.FollowerContact follower : followers) {
            if (follower.id() == null || follower.id().equals(request.getAuthorId())) {
                continue;
            }
            Notification n = repository.save(Notification.builder()
                    .userId(follower.id())
                    .type("FOLLOW_NEW_POST")
                    .title(title)
                    .body(body)
                    .linkUrl(link)
                    .actorUsername(request.getAuthorUsername())
                    .resourceType("POST")
                    .resourceId(String.valueOf(request.getPostId()))
                    .build());
            mailService.sendNewPostAlert(follower.email(), request.getAuthorUsername(),
                    request.getPostTitle(), link);
            log.debug("Notified follower={} notifId={} postId={}", follower.id(), n.getId(), request.getPostId());
        }
        log.info("Post-created fanout postId={} followersNotified={}", request.getPostId(), followers.size());
    }

    @Override
    @Transactional
    public NotificationResponse notifyUser(SystemNotifyRequest request) {
        Notification n = repository.save(Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .title(request.getTitle())
                .body(request.getBody())
                .linkUrl(request.getLinkUrl())
                .actorUsername(request.getActorUsername())
                .resourceType(request.getResourceType())
                .resourceId(request.getResourceId())
                .build());
        return toResponse(n);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(Long userId, boolean unreadOnly, String type, String q, Pageable pageable) {
        String t = type == null || type.isBlank() ? null : type.trim();
        String query = q == null || q.isBlank() ? null : q.trim();
        return repository.search(userId, unreadOnly, t, query, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification n = repository.findById(notificationId)
                .orElseThrow(() -> new ApiException("Notification not found", HttpStatus.NOT_FOUND.value()));
        if (!n.getUserId().equals(userId)) {
            throw new ApiException("Not allowed", HttpStatus.FORBIDDEN.value());
        }
        if (n.getReadAt() == null) {
            n.setReadAt(Instant.now());
            n = repository.save(n);
        }
        return toResponse(n);
    }

    @Override
    @Transactional
    public int markAllRead(Long userId) {
        Page<Notification> page = repository.search(userId, true, null, null, Pageable.unpaged());
        Instant now = Instant.now();
        int count = 0;
        for (Notification n : page) {
            n.setReadAt(now);
            count++;
        }
        repository.saveAll(page);
        return count;
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .linkUrl(n.getLinkUrl())
                .actorUsername(n.getActorUsername())
                .resourceType(n.getResourceType())
                .resourceId(n.getResourceId())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .read(n.getReadAt() != null)
                .build();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
