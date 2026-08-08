package com.threadly.community.service;

import com.threadly.community.dto.CommunityMessageResponse;
import com.threadly.community.dto.CreateChatMessageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CommunityChatService {
    Page<CommunityMessageResponse> list(UUID communityId, Pageable pageable);

    CommunityMessageResponse post(UUID communityId, UUID userId, String username, CreateChatMessageRequest request);

    /** Persist + STOMP broadcast (used by REST and WebSocket). */
    CommunityMessageResponse postAndBroadcast(UUID communityId, UUID userId, String username, CreateChatMessageRequest request);
}
