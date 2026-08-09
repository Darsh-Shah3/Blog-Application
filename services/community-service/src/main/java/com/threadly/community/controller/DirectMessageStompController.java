package com.threadly.community.controller;

import com.threadly.community.dto.CreateChatMessageRequest;
import com.threadly.community.security.ChatPrincipal;
import com.threadly.community.service.DirectMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * STOMP: client sends to {@code /app/dm/{threadId}}.
 * Broadcast: {@code /topic/dm.{threadId}}.
 */
@Controller
@RequiredArgsConstructor
public class DirectMessageStompController {

    private final DirectMessageService directMessageService;

    @MessageMapping("/dm/{threadId}")
    public void send(
            @DestinationVariable String threadId,
            @Valid @Payload CreateChatMessageRequest request,
            Principal principal) {
        ChatPrincipal chatUser = resolve(principal);
        if (chatUser == null) {
            throw new IllegalArgumentException("Sign in to send messages");
        }
        directMessageService.sendAndBroadcast(
                chatUser.userId(),
                chatUser.username(),
                UUID.fromString(threadId),
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
