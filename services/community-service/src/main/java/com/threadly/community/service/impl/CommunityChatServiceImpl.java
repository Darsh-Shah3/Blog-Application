package com.threadly.community.service.impl;

import com.threadly.community.client.NotificationClient;
import com.threadly.community.dto.CommunityMessageResponse;
import com.threadly.community.dto.CreateChatMessageRequest;
import com.threadly.community.entity.Community;
import com.threadly.community.entity.CommunityMessage;
import com.threadly.community.entity.Membership;
import com.threadly.community.exception.ApiException;
import com.threadly.community.repository.CommunityMessageRepository;
import com.threadly.community.repository.CommunityRepository;
import com.threadly.community.repository.MembershipRepository;
import com.threadly.community.service.CommunityChatService;
import com.threadly.community.util.AuditActors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityChatServiceImpl implements CommunityChatService {

    public static String topic(UUID communityId) {
        return "/topic/community." + communityId;
    }

    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;
    private final CommunityMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationClient notificationClient;

    private static final int MAX_CHAT_NOTIFY = 100;

    @Override
    @Transactional(readOnly = true)
    public Page<CommunityMessageResponse> list(UUID communityId, Pageable pageable) {
        if (!communityRepository.existsById(communityId)) {
            throw new ApiException("Community not found", HttpStatus.NOT_FOUND.value());
        }
        Pageable capped = pageable.getPageSize() > 100
                ? org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort())
                : pageable;
        return messageRepository.findByCommunityIdOrderByCreatedAtDesc(communityId, capped).map(this::toResponse);
    }

    @Override
    @Transactional
    public CommunityMessageResponse post(UUID communityId, UUID userId, String username, CreateChatMessageRequest request) {
        return postAndBroadcast(communityId, userId, username, request);
    }

    @Override
    @Transactional
    public CommunityMessageResponse postAndBroadcast(
            UUID communityId, UUID userId, String username, CreateChatMessageRequest request) {
        ChatWriteResult result = persist(communityId, userId, username, request);
        messagingTemplate.convertAndSend(topic(communityId), result.response());
        notifyMembers(result);
        return result.response();
    }

    private void notifyMembers(ChatWriteResult result) {
        List<UUID> recipients = membershipRepository.findByCommunityId(result.communityId()).stream()
                .map(Membership::getUserId)
                .filter(id -> id != null && !id.equals(result.authorId()))
                .limit(MAX_CHAT_NOTIFY)
                .toList();
        if (recipients.isEmpty()) {
            return;
        }
        notificationClient.notifyCommunityChat(
                result.communityId(),
                result.communityName(),
                result.communitySlug(),
                result.authorId(),
                result.authorUsername(),
                result.response().getId(),
                result.response().getBody(),
                recipients);
    }

    private ChatWriteResult persist(
            UUID communityId, UUID userId, String username, CreateChatMessageRequest request) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ApiException("Community not found", HttpStatus.NOT_FOUND.value()));
        if (!membershipRepository.existsByCommunityIdAndUserId(communityId, userId)) {
            throw new ApiException("Join this community to chat", HttpStatus.FORBIDDEN.value());
        }
        String body = request.getBody() == null ? "" : request.getBody().trim();
        if (body.isEmpty()) {
            throw new ApiException("Message cannot be empty", HttpStatus.BAD_REQUEST.value());
        }
        String name = AuditActors.resolve(username, userId);

        Map<String, Object> communitySnap = new LinkedHashMap<>();
        communitySnap.put("id", community.getId().toString());
        communitySnap.put("name", community.getName());
        communitySnap.put("slug", community.getSlug());

        Map<String, Object> authorSnap = new LinkedHashMap<>();
        authorSnap.put("id", userId.toString());
        authorSnap.put("username", name);

        CommunityMessage msg = CommunityMessage.builder()
                .communityId(communityId)
                .community(communitySnap)
                .authorId(userId)
                .author(authorSnap)
                .body(body)
                .build();
        CommunityMessageResponse response = toResponse(messageRepository.save(msg));
        return new ChatWriteResult(
                communityId,
                community.getName(),
                community.getSlug(),
                userId,
                name,
                response);
    }

    private record ChatWriteResult(
            UUID communityId,
            String communityName,
            String communitySlug,
            UUID authorId,
            String authorUsername,
            CommunityMessageResponse response) {
    }

    private CommunityMessageResponse toResponse(CommunityMessage m) {
        String username = m.getAuthor() != null && m.getAuthor().get("username") != null
                ? String.valueOf(m.getAuthor().get("username"))
                : "user";
        return CommunityMessageResponse.builder()
                .id(m.getId())
                .communityId(m.getCommunityId())
                .authorId(m.getAuthorId())
                .authorUsername(username)
                .author(m.getAuthor())
                .body(m.getBody())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
