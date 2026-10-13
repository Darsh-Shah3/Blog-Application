package com.threadly.notification.service.impl;

import com.threadly.notification.cache.UnreadCountCache;
import com.threadly.notification.client.AuthClient;
import com.threadly.notification.dto.CommunityChatEventRequest;
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
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository repository;
    private final AuthClient authClient;
    private final OutboundMailService mailService;
    private final UnreadCountCache unreadCountCache;

    @Override
    @Transactional
    public void onPostCreated(PostCreatedEventRequest request) {
        List<AuthClient.FollowerContact> followers = authClient.listFollowers(request.getAuthorId());
        if (followers.isEmpty()) {
            log.info("No followers to notify for postId={}", request.getPostId());
            return;
        }
        String link = "/post/" + request.getPostId();
        String title = request.getPostTitle();
        String summary = request.getSummary() != null && !request.getSummary().isBlank()
                ? request.getSummary()
                : ("New post from @" + request.getAuthorUsername());

        for (AuthClient.FollowerContact follower : followers) {
            if (follower.id() == null || follower.id().equals(request.getAuthorId())) {
                continue;
            }
            Notification n = repository.save(Notification.builder()
                    .userId(follower.id())
                    .recipient(Map.of(
                            "id", follower.id().toString(),
                            "username", follower.username() == null ? "" : follower.username()))
                    .type("FOLLOW_NEW_POST")
                    .title(title)
                    .body(summary)
                    .summary(summary)
                    .linkUrl(link)
                    .mediaUrl(request.getMediaUrl())
                    .actorUsername(request.getAuthorUsername())
                    .postTitle(request.getPostTitle())
                    .resourceType("POST")
                    .resourceId(String.valueOf(request.getPostId()))
                    .build());
            unreadCountCache.increment(follower.id(), "FOLLOW_NEW_POST");
            mailService.sendNewPostAlert(follower.email(), request.getAuthorUsername(),
                    request.getPostTitle(), link);
            log.debug("Notified follower={} notifId={}", follower.id(), n.getId());
        }
        log.info("Post-created fanout postId={} followers={}", request.getPostId(), followers.size());
    }

    @Override
    @Transactional
    public void onCommunityChatMessage(CommunityChatEventRequest request) {
        List<UUID> recipients = request.getRecipientUserIds() == null
                ? List.of()
                : request.getRecipientUserIds();
        if (recipients.isEmpty()) {
            log.info("No recipients for community chat communityId={}", request.getCommunityId());
            return;
        }
        String author = request.getAuthorUsername() == null || request.getAuthorUsername().isBlank()
                ? "someone"
                : request.getAuthorUsername();
        String title = author + " in " + request.getCommunityName();
        String preview = request.getBodyPreview() == null ? "" : request.getBodyPreview().trim();
        if (preview.length() > 160) {
            preview = preview.substring(0, 157) + "...";
        }
        String body = preview.isEmpty()
                ? ("New message in " + request.getCommunityName())
                : preview;
        String link = "/c/" + request.getCommunitySlug();
        int saved = 0;
        for (UUID userId : recipients) {
            if (userId == null || userId.equals(request.getAuthorId())) {
                continue;
            }
            repository.save(Notification.builder()
                    .userId(userId)
                    .recipient(Map.of("id", userId.toString(), "username", ""))
                    .type("COMMUNITY_CHAT")
                    .title(title)
                    .body(body)
                    .summary(body)
                    .linkUrl(link)
                    .actorUsername(author)
                    .resourceType("COMMUNITY_MESSAGE")
                    .resourceId(String.valueOf(request.getMessageId()))
                    .build());
            unreadCountCache.increment(userId, "COMMUNITY_CHAT");
            saved++;
        }
        log.info("Community-chat fanout communityId={} notified={}", request.getCommunityId(), saved);
    }

    @Override
    @Transactional
    public NotificationResponse notifyUser(SystemNotifyRequest request) {
        Notification n = repository.save(Notification.builder()
                .userId(request.getUserId())
                .recipient(Map.of("id", request.getUserId().toString(), "username", ""))
                .type(request.getType())
                .title(request.getTitle())
                .body(request.getBody())
                .summary(request.getBody())
                .linkUrl(request.getLinkUrl())
                .actorUsername(request.getActorUsername())
                .resourceType(request.getResourceType())
                .resourceId(request.getResourceId())
                .build());
        unreadCountCache.increment(request.getUserId(), request.getType());
        return toResponse(n);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(UUID userId, boolean unreadOnly, String type, String q, Pageable pageable) {
        String t = type == null || type.isBlank() ? null : type.trim();
        // Pre-build LIKE pattern in Java so Postgres never lower(bytea) from Hibernate concat.
        String pattern = null;
        if (q != null && !q.isBlank()) {
            pattern = "%" + q.trim().toLowerCase(java.util.Locale.ROOT) + "%";
        }
        return repository.search(userId, unreadOnly, t, pattern, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return unreadCountCache.get(userId, null).orElseGet(() -> {
            long count = repository.countByUserIdAndReadAtIsNull(userId);
            unreadCountCache.set(userId, null, count);
            return count;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(UUID userId, String type) {
        if (type == null || type.isBlank()) {
            return unreadCount(userId);
        }
        String t = type.trim();
        return unreadCountCache.get(userId, t).orElseGet(() -> {
            long count = repository.countByUserIdAndTypeAndReadAtIsNull(userId, t);
            unreadCountCache.set(userId, t, count);
            return count;
        });
    }

    @Override
    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        Notification n = repository.findById(notificationId)
                .orElseThrow(() -> new ApiException("Notification not found", HttpStatus.NOT_FOUND.value()));
        if (!n.getUserId().equals(userId)) {
            throw new ApiException("Not allowed", HttpStatus.FORBIDDEN.value());
        }
        if (n.getReadAt() == null) {
            n.setReadAt(Instant.now());
            n = repository.save(n);
            unreadCountCache.decrement(userId, n.getType());
        }
        return toResponse(n);
    }

    @Override
    @Transactional
    public int markAllRead(UUID userId) {
        Page<Notification> page = repository.search(userId, true, null, null, Pageable.unpaged());
        Instant now = Instant.now();
        int count = 0;
        for (Notification n : page) {
            n.setReadAt(now);
            count++;
        }
        repository.saveAll(page);
        unreadCountCache.invalidateAll(userId);
        return count;
    }

    @Override
    @Transactional
    public int markReadMatching(UUID userId, String type, String actorUsername) {
        String t = type == null || type.isBlank() ? null : type.trim();
        String actor = actorUsername == null || actorUsername.isBlank() ? null : actorUsername.trim();
        int updated = repository.markReadMatching(userId, t, actor, Instant.now());
        unreadCountCache.invalidate(userId, t);
        unreadCountCache.invalidateAll(userId);
        return updated;
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .recipient(n.getRecipient())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .summary(n.getSummary())
                .linkUrl(n.getLinkUrl())
                .mediaUrl(n.getMediaUrl())
                .actorUsername(n.getActorUsername())
                .postTitle(n.getPostTitle())
                .resourceType(n.getResourceType())
                .resourceId(n.getResourceId())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .read(n.getReadAt() != null)
                .build();
    }
}
