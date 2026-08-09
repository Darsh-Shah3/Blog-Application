package com.threadly.community.controller;

import com.threadly.community.dto.CreateChatMessageRequest;
import com.threadly.community.dto.DmMessageResponse;
import com.threadly.community.dto.DmThreadResponse;
import com.threadly.community.dto.OpenDmRequest;
import com.threadly.community.service.DirectMessageService;
import com.threadly.community.web.RequestIdentity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dm")
@RequiredArgsConstructor
public class DirectMessageController {

    private final DirectMessageService directMessageService;
    private final RequestIdentity requestIdentity;

    @GetMapping("/threads")
    public ResponseEntity<List<DmThreadResponse>> listThreads(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "X-Username", required = false) String username) {
        UUID me = requestIdentity.requireUserId(userId);
        return ResponseEntity.ok(directMessageService.listThreads(me, username));
    }

    @PostMapping("/threads")
    public ResponseEntity<DmThreadResponse> openThread(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "X-Username", required = false) String username,
            @Valid @RequestBody OpenDmRequest request) {
        UUID me = requestIdentity.requireUserId(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(directMessageService.openThread(me, username, request));
    }

    @GetMapping("/threads/{threadId}")
    public ResponseEntity<DmThreadResponse> getThread(
            @PathVariable UUID threadId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID me = requestIdentity.requireUserId(userId);
        return ResponseEntity.ok(directMessageService.getThread(me, threadId));
    }

    @GetMapping("/threads/{threadId}/messages")
    public ResponseEntity<Page<DmMessageResponse>> listMessages(
            @PathVariable UUID threadId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PageableDefault(size = 50) Pageable pageable) {
        UUID me = requestIdentity.requireUserId(userId);
        return ResponseEntity.ok(directMessageService.listMessages(me, threadId, pageable));
    }

    @PostMapping("/threads/{threadId}/messages")
    public ResponseEntity<DmMessageResponse> send(
            @PathVariable UUID threadId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "X-Username", required = false) String username,
            @Valid @RequestBody CreateChatMessageRequest request) {
        UUID me = requestIdentity.requireUserId(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(directMessageService.send(me, username, threadId, request));
    }
}
