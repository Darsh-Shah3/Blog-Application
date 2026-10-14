package com.threadly.community.service;

import com.threadly.community.dto.CommunityMessageResponse;
import com.threadly.community.dto.CreateChatMessageRequest;
import com.threadly.community.dto.DeleteMessageRequest;
import com.threadly.community.dto.UpdateChatMessageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CommunityChatService {
    Page<CommunityMessageResponse> list(UUID communityId, UUID viewerId, Pageable pageable);

    CommunityMessageResponse post(UUID communityId, UUID userId, String username, CreateChatMessageRequest request);

    /** Persist + STOMP broadcast (used by REST and WebSocket). */
    CommunityMessageResponse postAndBroadcast(UUID communityId, UUID userId, String username, CreateChatMessageRequest request);

    CommunityMessageResponse edit(UUID communityId, UUID userId, UUID messageId, UpdateChatMessageRequest request);

    /** Returns null when scope=ME; otherwise broadcasted update/tombstone. */
    CommunityMessageResponse delete(UUID communityId, UUID userId, UUID messageId, DeleteMessageRequest request);
}
