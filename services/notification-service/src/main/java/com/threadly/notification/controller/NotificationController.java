package com.threadly.notification.controller;

import com.threadly.notification.dto.NotificationResponse;
import com.threadly.notification.dto.PostCreatedEventRequest;
import com.threadly.notification.dto.SystemNotifyRequest;
import com.threadly.notification.exception.ApiException;
import com.threadly.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/api/v1/internal/notifications/post-created")
    @PreAuthorize("hasRole('INTERNAL')")
    public ResponseEntity<Map<String, String>> postCreated(@Valid @RequestBody PostCreatedEventRequest request) {
        notificationService.onPostCreated(request);
        return ResponseEntity.accepted().body(Map.of("status", "accepted"));
    }

    @PostMapping("/api/v1/internal/notifications")
    @PreAuthorize("hasRole('INTERNAL')")
    public ResponseEntity<NotificationResponse> systemNotify(@Valid @RequestBody SystemNotifyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.notifyUser(request));
    }

    @GetMapping("/api/v1/notifications")
    public ResponseEntity<Page<NotificationResponse>> list(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 30) Pageable pageable) {
        return ResponseEntity.ok(notificationService.list(requireUser(userId), unreadOnly, type, q, pageable));
    }

    @GetMapping("/api/v1/notifications/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(requireUser(userId))));
    }

    @PostMapping("/api/v1/notifications/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markRead(requireUser(userId), id));
    }

    @PostMapping("/api/v1/notifications/read-all")
    public ResponseEntity<Map<String, Integer>> markAll(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(Map.of("marked", notificationService.markAllRead(requireUser(userId))));
    }

    private Long requireUser(Long userId) {
        if (userId == null) {
            throw new ApiException("Authentication required", 401);
        }
        return userId;
    }
}
