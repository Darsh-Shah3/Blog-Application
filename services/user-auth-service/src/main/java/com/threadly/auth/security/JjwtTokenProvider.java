package com.threadly.auth.security;

import com.threadly.auth.config.AppSecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HS256 JWT adapter. Secret/TTL come from configuration so secrets stay out of code.
 */
@Component
public class JjwtTokenProvider implements JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JjwtTokenProvider.class);

    private final Key key;
    private final long expirationMs;

    public JjwtTokenProvider(AppSecurityProperties props) {
        this.key = Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = props.getJwt().getExpirationMs();
    }

    @Override
    public String generateToken(java.util.UUID userId, String username, List<String> roles, List<String> permissions) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
                .setId(jti)
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(exp)
                .addClaims(Map.of(
                        "username", username,
                        "roles", roles,
                        "permissions", permissions == null ? List.of() : permissions
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            log.debug("JWT invalid: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    @Override
    public String extractUsername(String token) {
        return parseClaims(token).get("username", String.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = parseClaims(token).get("roles");
        if (roles instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return Collections.emptyList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        Object permissions = parseClaims(token).get("permissions");
        if (permissions instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return Collections.emptyList();
    }

    @Override
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Override
    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    @Override
    public Duration remainingTtl(String token) {
        Date exp = parseClaims(token).getExpiration();
        if (exp == null) {
            return Duration.ofMillis(expirationMs);
        }
        long ms = exp.getTime() - System.currentTimeMillis();
        return ms > 0 ? Duration.ofMillis(ms) : Duration.ZERO;
    }
}
