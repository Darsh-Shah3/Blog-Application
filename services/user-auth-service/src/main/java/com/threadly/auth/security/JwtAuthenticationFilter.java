package com.threadly.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Translates Bearer JWT into Spring Security context.
 * Downstream code should use {@code Authentication}, not raw Authorization headers.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7).trim();
            if (jwtTokenProvider.isValid(token)) {
                String userId = jwtTokenProvider.extractUserId(token);
                String username = jwtTokenProvider.extractUsername(token);
                var authorities = Stream.concat(
                                jwtTokenProvider.extractRoles(token).stream(),
                                jwtTokenProvider.extractPermissions(token).stream())
                        .distinct()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(userId, null, authorities));
                MDC.put("userId", userId);
                if (username != null && !username.isBlank()) {
                    MDC.put("username", username);
                }
                log.debug("JWT accepted userId={} username={} path={}", userId, username, request.getRequestURI());
            } else {
                log.warn("Invalid JWT rejected path={}", request.getRequestURI());
            }
        }
        filterChain.doFilter(request, response);
    }
}
