package com.threadly.auth.security;

import com.threadly.auth.config.AppSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InternalApiKeyFilter.class);
    private final AppSecurityProperties properties;

    public InternalApiKeyFilter(AppSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = request.getHeader("X-Internal-Api-Key");
        if (key != null && key.equals(properties.getInternalApiKey())
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Spring adds ROLE_ prefix check for hasRole("INTERNAL") via hasAuthority("ROLE_INTERNAL").
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            "internal-service", null,
                            List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))));
            MDC.put("userId", "internal");
            log.debug("Internal API key accepted path={}", request.getRequestURI());
        }
        filterChain.doFilter(request, response);
    }
}
