package com.threadly.community.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Authenticates STOMP CONNECT with JWT from Authorization header or {@code token} header.
 * Guests may connect without a token (history is still loaded over REST).
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String auth = firstHeader(accessor, "Authorization");
        String token = null;
        if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = auth.substring(7).trim();
        }
        if (token == null || token.isBlank()) {
            token = firstHeader(accessor, "token");
        }
        if (token == null || token.isBlank()) {
            return message;
        }

        try {
            Claims claims = jwtService.parse(token);
            String userId = claims.getSubject();
            if (userId == null || userId.isBlank()) {
                return message;
            }
            String username = claims.get("username", String.class);
            List<String> roles = jwtService.roles(claims);
            var authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            ChatPrincipal principal = new ChatPrincipal(UUID.fromString(userId), username);
            accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, authorities));
        } catch (Exception ignored) {
            // leave unauthenticated — SEND will be rejected in the controller
        }
        return message;
    }

    private static String firstHeader(StompHeaderAccessor accessor, String name) {
        List<String> values = accessor.getNativeHeader(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }
}
