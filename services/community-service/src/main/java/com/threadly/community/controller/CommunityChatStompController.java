package com.threadly.community.controller;

import com.threadly.community.dto.CommunityMessageResponse;
import com.threadly.community.dto.CreateChatMessageRequest;
import com.threadly.community.security.ChatPrincipal;
import com.threadly.community.service.CommunityChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * STOMP entry: client sends to {@code /app/chat/{communityId}}.
 * Broadcast goes to {@code /topic/community.{communityId}}.
 */
@Controller
@RequiredArgsConstructor
public class CommunityChatStompController {

    private final CommunityChatService chatService;

    @MessageMapping("/chat/{communityId}")
    public void send(
            @DestinationVariable String communityId,
            @Valid @Payload CreateChatMessageRequest request,
            Principal principal) {
        ChatPrincipal chatUser = resolve(principal);
        if (chatUser == null) {
            throw new IllegalArgumentException("Sign in to chat");
        }
        chatService.postAndBroadcast(
                java.util.UUID.fromString(communityId),
                chatUser.userId(),
                chatUser.username(),
                request);
    }

    private static ChatPrincipal resolve(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof ChatPrincipal chat) {
            return chat;
        }
        if (principal instanceof ChatPrincipal chat) {
            return chat;
        }
        return null;
    }
}
