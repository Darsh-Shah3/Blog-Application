package com.threadly.auth.service;

import com.threadly.auth.dto.OAuthStartResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/**
 * Starts Google OAuth authorize URLs when {@code app.oauth.google.client-id} is set.
 */
@Service
public class OAuthService {

    @Value("${app.oauth.google.client-id:}")
    private String googleClientId;

    @Value("${app.oauth.redirect-base-url:http://localhost:3000}")
    private String redirectBaseUrl;

    public OAuthStartResponse start(String providerRaw, String returnTo) {
        String provider = providerRaw == null ? "" : providerRaw.trim().toLowerCase(Locale.ROOT);
        if (!"google".equals(provider)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only Google OAuth is supported.");
        }
        return startGoogle(returnTo);
    }

    private OAuthStartResponse startGoogle(String returnTo) {
        if (isBlank(googleClientId)) {
            return OAuthStartResponse.builder()
                    .provider("google")
                    .configured(false)
                    .message("Google sign-in is available once OAuth credentials are set "
                            + "(set OAUTH_GOOGLE_CLIENT_ID). Email login still works.")
                    .build();
        }
        String redirect = redirectUri("google");
        String state = stateToken("google", returnTo);
        String url = UriComponentsBuilder
                .fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", redirect)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("access_type", "online")
                .queryParam("prompt", "select_account")
                .queryParam("state", state)
                .build(true)
                .toUriString();
        return OAuthStartResponse.builder()
                .provider("google")
                .configured(true)
                .authorizationUrl(url)
                .build();
    }

    private String redirectUri(String provider) {
        String base = redirectBaseUrl == null ? "http://localhost:3000" : redirectBaseUrl.replaceAll("/+$", "");
        return base + "/oauth/callback/" + provider;
    }

    private static String stateToken(String provider, String returnTo) {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String rt = returnTo == null ? "" : returnTo;
        return URLEncoder.encode(provider + ":" + nonce + ":" + rt, StandardCharsets.UTF_8);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
