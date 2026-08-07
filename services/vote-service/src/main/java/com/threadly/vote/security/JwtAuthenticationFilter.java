package com.threadly.vote.security;

import io.jsonwebtoken.Claims;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7).trim();
            if (jwtService.isValid(token)) {
                Claims claims = jwtService.parse(token);
                String userId = claims.getSubject();
                List<String> roles = jwtService.roles(claims);
                List<String> permissions = jwtService.permissions(claims);
                var authorities = Stream.concat(roles.stream(), permissions.stream())
                        .distinct()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(userId, null, authorities));
                String username = claims.get("username", String.class);
                MDC.put("userId", userId);
                if (username != null && !username.isBlank()) {
                    MDC.put("username", username);
                }
                log.debug("JWT accepted userId={} username={} path={}", userId, username, request.getRequestURI());
                var headers = new java.util.HashMap<String, String>();
                headers.put("X-User-Id", userId);
                headers.put("X-User-Roles", String.join(",", roles));
                headers.put("X-User-Permissions", String.join(",", permissions));
                if (username != null && !username.isBlank()) {
                    headers.put("X-Username", username);
                }
                request = new HeaderInjectingRequest(request, headers);
            } else {
                log.warn("Rejected invalid JWT path={}", request.getRequestURI());
            }
        }
        filterChain.doFilter(request, response);
    }
}
