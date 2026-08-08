package com.threadly.community.controller;

import com.threadly.community.dto.CommunityMessageResponse;
import com.threadly.community.dto.CreateChatMessageRequest;
import com.threadly.community.service.CommunityChatService;
import com.threadly.community.web.RequestIdentity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/messages")
@RequiredArgsConstructor
public class CommunityChatController {

    private final CommunityChatService chatService;
    private final RequestIdentity requestIdentity;

    @GetMapping
    public ResponseEntity<Page<CommunityMessageResponse>> list(
            @PathVariable UUID communityId,
            @PageableDefault(size = 40) Pageable pageable) {
        return ResponseEntity.ok(chatService.list(communityId, pageable));
    }

    @PostMapping
    public ResponseEntity<CommunityMessageResponse> post(
            @PathVariable UUID communityId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "X-Username", required = false) String username,
            @Valid @RequestBody CreateChatMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                chatService.post(communityId, requestIdentity.requireUserId(userId), username, request));
    }
}
