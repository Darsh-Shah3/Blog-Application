package com.threadly.community.service;

import com.threadly.community.dto.CreateChatMessageRequest;
import com.threadly.community.dto.DeleteMessageRequest;
import com.threadly.community.dto.DmMessageResponse;
import com.threadly.community.dto.DmThreadResponse;
import com.threadly.community.dto.OpenDmRequest;
import com.threadly.community.dto.UpdateChatMessageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DirectMessageService {

    List<DmThreadResponse> listThreads(UUID me, String myUsername);

    DmThreadResponse openThread(UUID me, String myUsername, OpenDmRequest request);

    DmThreadResponse getThread(UUID me, UUID threadId);

    Page<DmMessageResponse> listMessages(UUID me, UUID threadId, Pageable pageable);

    DmMessageResponse send(UUID me, String myUsername, UUID threadId, CreateChatMessageRequest request);

    DmMessageResponse sendAndBroadcast(UUID me, String myUsername, UUID threadId, CreateChatMessageRequest request);

    DmMessageResponse edit(UUID me, UUID threadId, UUID messageId, UpdateChatMessageRequest request);

    /** Returns null when scope=ME (caller should drop locally); otherwise broadcasted tombstone/update. */
    DmMessageResponse delete(UUID me, UUID threadId, UUID messageId, DeleteMessageRequest request);
}
