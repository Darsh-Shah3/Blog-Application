package com.threadly.comment.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collections;
import java.util.List;

@Component
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private final Key key;
    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    public Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
    public boolean isValid(String token) {
        try { parse(token); return true; } catch (Exception e) { log.debug("JWT invalid: {}", e.getMessage()); return false; }
    }
    public List<String> roles(Claims claims) {
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list) return list.stream().map(String::valueOf).toList();
        return Collections.emptyList();
    }

    public List<String> permissions(Claims claims) {
        Object permissions = claims.get("permissions");
        if (permissions instanceof List<?> list) return list.stream().map(String::valueOf).toList();
        return Collections.emptyList();
    }
}
