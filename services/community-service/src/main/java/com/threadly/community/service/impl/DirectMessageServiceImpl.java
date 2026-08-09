package com.threadly.community.service.impl;

import com.threadly.community.client.NotificationClient;
import com.threadly.community.dto.CreateChatMessageRequest;
import com.threadly.community.dto.DmMessageResponse;
import com.threadly.community.dto.DmThreadResponse;
import com.threadly.community.dto.OpenDmRequest;
import com.threadly.community.entity.DmMessage;
import com.threadly.community.entity.DmThread;
import com.threadly.community.exception.ApiException;
import com.threadly.community.repository.DmMessageRepository;
import com.threadly.community.repository.DmThreadRepository;
import com.threadly.community.service.DirectMessageService;
import com.threadly.community.util.AuditActors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DirectMessageServiceImpl implements DirectMessageService {

    public static String topic(UUID threadId) {
        return "/topic/dm." + threadId;
    }

    private final DmThreadRepository threadRepository;
    private final DmMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationClient notificationClient;

    @Override
    @Transactional(readOnly = true)
    public List<DmThreadResponse> listThreads(UUID me, String myUsername) {
        return threadRepository.findInboxForUser(me).stream()
                .map(t -> toThreadResponse(t, me))
                .toList();
    }

    @Override
    @Transactional
    public DmThreadResponse openThread(UUID me, String myUsername, OpenDmRequest request) {
        UUID peer = request.getPeerUserId();
        if (peer == null) {
            throw new ApiException("peerUserId is required", HttpStatus.BAD_REQUEST.value());
        }
        if (peer.equals(me)) {
            throw new ApiException("You cannot message yourself", HttpStatus.BAD_REQUEST.value());
        }
        String myName = AuditActors.resolve(myUsername, me);
        String peerName = request.getPeerUsername() == null || request.getPeerUsername().isBlank()
                ? "user"
                : request.getPeerUsername().trim();

        UUID low;
        UUID high;
        // PostgreSQL UUID order is unsigned-byte order; Java UUID.compareTo is signed — don't use it.
        if (compareUuidPg(me, peer) < 0) {
            low = me;
            high = peer;
        } else {
            low = peer;
            high = me;
        }
        String lowName = low.equals(me) ? myName : peerName;
        String highName = high.equals(me) ? myName : peerName;

        DmThread thread = threadRepository.findByUserLowAndUserHigh(low, high)
                .orElseGet(() -> threadRepository.save(DmThread.builder()
                        .userLow(low)
                        .userHigh(high)
                        .userLowUsername(lowName)
                        .userHighUsername(highName)
                        .build()));

        // Refresh username snapshots when the client provides them
        if (low.equals(me)) {
            thread.setUserLowUsername(myName);
            if (!"user".equals(peerName)) {
                thread.setUserHighUsername(peerName);
            }
        } else {
            thread.setUserHighUsername(myName);
            if (!"user".equals(peerName)) {
                thread.setUserLowUsername(peerName);
            }
        }
        return toThreadResponse(threadRepository.save(thread), me);
    }

    /**
     * Same ordering as PostgreSQL {@code UUID} comparisons (unsigned raw bytes),
     * not {@link UUID#compareTo} which uses signed longs and can invert pairs.
     */
    static int compareUuidPg(UUID a, UUID b) {
        long am = a.getMostSignificantBits();
        long bm = b.getMostSignificantBits();
        int c = Long.compareUnsigned(am, bm);
        if (c != 0) {
            return c;
        }
        return Long.compareUnsigned(a.getLeastSignificantBits(), b.getLeastSignificantBits());
    }

    @Override
    @Transactional(readOnly = true)
    public DmThreadResponse getThread(UUID me, UUID threadId) {
        return toThreadResponse(requireParticipant(me, threadId), me);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DmMessageResponse> listMessages(UUID me, UUID threadId, Pageable pageable) {
        requireParticipant(me, threadId);
        Pageable capped = pageable.getPageSize() > 100
                ? PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort())
                : pageable;
        return messageRepository.findByThreadIdOrderByCreatedAtDesc(threadId, capped).map(this::toMessageResponse);
    }

    @Override
    @Transactional
    public DmMessageResponse send(UUID me, String myUsername, UUID threadId, CreateChatMessageRequest request) {
        return sendAndBroadcast(me, myUsername, threadId, request);
    }

    @Override
    @Transactional
    public DmMessageResponse sendAndBroadcast(UUID me, String myUsername, UUID threadId, CreateChatMessageRequest request) {
        DmThread thread = requireParticipant(me, threadId);
        String body = request.getBody() == null ? "" : request.getBody().trim();
        if (body.isEmpty()) {
            throw new ApiException("Message cannot be empty", HttpStatus.BAD_REQUEST.value());
        }
        String name = AuditActors.resolve(myUsername, me);

        Map<String, Object> authorSnap = new LinkedHashMap<>();
        authorSnap.put("id", me.toString());
        authorSnap.put("username", name);

        DmMessage msg = DmMessage.builder()
                .threadId(threadId)
                .authorId(me)
                .author(authorSnap)
                .body(body)
                .build();
        DmMessageResponse response = toMessageResponse(messageRepository.save(msg));

        thread.setLastMessageAt(Instant.now());
        thread.setLastMessagePreview(body.length() > 200 ? body.substring(0, 197) + "…" : body);
        if (me.equals(thread.getUserLow())) {
            thread.setUserLowUsername(name);
        } else {
            thread.setUserHighUsername(name);
        }
        threadRepository.save(thread);

        messagingTemplate.convertAndSend(topic(threadId), response);

        UUID peerId = thread.peerOf(me);
        if (peerId != null) {
            String preview = body.length() > 160 ? body.substring(0, 157) + "…" : body;
            String link = "/messages?with=" + name;
            notificationClient.notifyUser(
                    peerId,
                    "DIRECT_MESSAGE",
                    "Message from @" + name,
                    preview,
                    link,
                    name,
                    "DM_MESSAGE",
                    response.getId() != null ? response.getId().toString() : null
            );
        }
        return response;
    }

    private DmThread requireParticipant(UUID me, UUID threadId) {
        DmThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ApiException("Conversation not found", HttpStatus.NOT_FOUND.value()));
        if (!thread.involves(me)) {
            throw new ApiException("Not a participant in this conversation", HttpStatus.FORBIDDEN.value());
        }
        return thread;
    }

    private DmThreadResponse toThreadResponse(DmThread t, UUID me) {
        return DmThreadResponse.builder()
                .id(t.getId())
                .peerUserId(t.peerOf(me))
                .peerUsername(t.peerUsernameOf(me))
                .lastMessagePreview(t.getLastMessagePreview())
                .lastMessageAt(t.getLastMessageAt())
                .createdAt(t.getCreatedAt())
                .build();
    }

    private DmMessageResponse toMessageResponse(DmMessage m) {
        String username = m.getAuthor() != null && m.getAuthor().get("username") != null
                ? String.valueOf(m.getAuthor().get("username"))
                : "user";
        return DmMessageResponse.builder()
                .id(m.getId())
                .threadId(m.getThreadId())
                .authorId(m.getAuthorId())
                .authorUsername(username)
                .author(m.getAuthor())
                .body(m.getBody())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
