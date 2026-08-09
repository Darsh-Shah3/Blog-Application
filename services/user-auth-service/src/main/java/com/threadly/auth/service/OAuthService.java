package com.threadly.auth.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.threadly.auth.dto.AuthResponse;
import com.threadly.auth.dto.OAuthStartResponse;
import com.threadly.auth.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/**
 * Google OAuth: authorize URL + code exchange + profile, then session via {@link AuthService}.
 */
@Service
@RequiredArgsConstructor
public class OAuthService {

    private static final Logger log = LoggerFactory.getLogger(OAuthService.class);
    private static final RestClient HTTP = RestClient.create();

    private final AuthService authService;

    @Value("${app.oauth.google.client-id:}")
    private String googleClientId;

    @Value("${app.oauth.google.client-secret:}")
    private String googleClientSecret;

    @Value("${app.oauth.redirect-base-url:http://localhost:3000}")
    private String redirectBaseUrl;

    public OAuthStartResponse start(String providerRaw, String returnTo) {
        String provider = normalizeProvider(providerRaw);
        if (!"google".equals(provider)) {
            throw new ApiException("Only Google OAuth is supported.", HttpStatus.BAD_REQUEST.value());
        }
        return startGoogle(returnTo);
    }

    public AuthResponse complete(String providerRaw, String code, String state) {
        String provider = normalizeProvider(providerRaw);
        if (!"google".equals(provider)) {
            throw new ApiException("Only Google OAuth is supported.", HttpStatus.BAD_REQUEST.value());
        }
        if (isBlank(code)) {
            throw new ApiException("Missing authorization code.", HttpStatus.BAD_REQUEST.value());
        }
        if (!isGoogleConfigured()) {
            throw new ApiException(
                    "Google sign-in is not fully configured (OAUTH_GOOGLE_CLIENT_ID and OAUTH_GOOGLE_CLIENT_SECRET).",
                    HttpStatus.SERVICE_UNAVAILABLE.value());
        }
        GoogleUserInfo profile = exchangeGoogleCode(code.trim());
        if (isBlank(profile.email())) {
            throw new ApiException("Google did not return an email address.", HttpStatus.BAD_REQUEST.value());
        }
        if (profile.emailVerified() != null && !profile.emailVerified()) {
            throw new ApiException("Google email is not verified.", HttpStatus.BAD_REQUEST.value());
        }
        log.info("Google OAuth success email={} sub={}", profile.email(), profile.sub());
        return authService.loginOrRegisterOAuth(
                profile.email(),
                profile.name(),
                profile.sub());
    }

    private OAuthStartResponse startGoogle(String returnTo) {
        if (!isGoogleConfigured()) {
            return OAuthStartResponse.builder()
                    .provider("google")
                    .configured(false)
                    .message("Google sign-in needs OAUTH_GOOGLE_CLIENT_ID and OAUTH_GOOGLE_CLIENT_SECRET. Email login still works.")
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
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
        return OAuthStartResponse.builder()
                .provider("google")
                .configured(true)
                .authorizationUrl(url)
                .build();
    }

    private GoogleUserInfo exchangeGoogleCode(String code) {
        String redirect = redirectUri("google");
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", googleClientId);
        form.add("client_secret", googleClientSecret);
        form.add("redirect_uri", redirect);
        form.add("grant_type", "authorization_code");

        GoogleTokenResponse token;
        try {
            token = HTTP.post()
                    .uri("https://oauth2.googleapis.com/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(GoogleTokenResponse.class);
        } catch (RestClientResponseException ex) {
            log.warn("Google token exchange failed status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ApiException("Google authorization failed. Try again.", HttpStatus.BAD_REQUEST.value());
        } catch (Exception ex) {
            log.error("Google token exchange error", ex);
            throw new ApiException("Could not reach Google for token exchange.", HttpStatus.BAD_GATEWAY.value());
        }
        if (token == null || isBlank(token.accessToken())) {
            throw new ApiException("Google did not return an access token.", HttpStatus.BAD_GATEWAY.value());
        }

        try {
            GoogleUserInfo info = HTTP.get()
                    .uri("https://openidconnect.googleapis.com/v1/userinfo")
                    .header("Authorization", "Bearer " + token.accessToken())
                    .retrieve()
                    .body(GoogleUserInfo.class);
            if (info == null) {
                throw new ApiException("Google userinfo was empty.", HttpStatus.BAD_GATEWAY.value());
            }
            return info;
        } catch (ApiException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.warn("Google userinfo failed status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ApiException("Could not load Google profile.", HttpStatus.BAD_GATEWAY.value());
        } catch (Exception ex) {
            log.error("Google userinfo error", ex);
            throw new ApiException("Could not load Google profile.", HttpStatus.BAD_GATEWAY.value());
        }
    }

    private String redirectUri(String provider) {
        String base = redirectBaseUrl == null ? "http://localhost:3000" : redirectBaseUrl.replaceAll("/+$", "");
        return base + "/oauth/callback/" + provider;
    }

    private static String stateToken(String provider, String returnTo) {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String rt = returnTo == null ? "" : returnTo;
        return provider + ":" + nonce + ":" + rt;
    }

    private boolean isGoogleConfigured() {
        return !isBlank(googleClientId) && !isBlank(googleClientSecret);
    }

    private static String normalizeProvider(String providerRaw) {
        return providerRaw == null ? "" : providerRaw.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("id_token") String idToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("error") String error,
            @JsonProperty("error_description") String errorDescription
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleUserInfo(
            String sub,
            String email,
            @JsonProperty("email_verified") Boolean emailVerified,
            String name,
            @JsonProperty("given_name") String givenName,
            String picture
    ) {
    }
}
