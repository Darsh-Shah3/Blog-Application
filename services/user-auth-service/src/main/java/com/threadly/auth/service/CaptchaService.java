package com.threadly.auth.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.threadly.auth.dto.CaptchaConfigResponse;
import com.threadly.auth.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Google reCAPTCHA v2 verification for public signup.
 * Disabled when site key or secret is blank (local dev without keys).
 */
@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);
    private static final String SITEVERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final RestClient HTTP = RestClient.create();

    @Value("${app.captcha.recaptcha.site-key:}")
    private String siteKey;

    @Value("${app.captcha.recaptcha.secret-key:}")
    private String secretKey;

    public boolean isEnabled() {
        return !isBlank(siteKey) && !isBlank(secretKey);
    }

    public CaptchaConfigResponse config() {
        return CaptchaConfigResponse.builder()
                .enabled(isEnabled())
                .provider(isEnabled() ? "recaptcha-v2" : null)
                .siteKey(isEnabled() ? siteKey.trim() : null)
                .build();
    }

    /**
     * Rejects signup when captcha is required and the token is missing or invalid.
     */
    public void verifySignupToken(String captchaToken, String remoteIp) {
        if (!isEnabled()) {
            return;
        }
        if (isBlank(captchaToken)) {
            throw new ApiException("Please complete the CAPTCHA.", HttpStatus.BAD_REQUEST.value());
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secretKey.trim());
        form.add("response", captchaToken.trim());
        if (!isBlank(remoteIp)) {
            form.add("remoteip", remoteIp.trim());
        }

        SiteVerifyResponse result;
        try {
            result = HTTP.post()
                    .uri(SITEVERIFY_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(SiteVerifyResponse.class);
        } catch (RestClientException ex) {
            log.error("reCAPTCHA siteverify failed: {}", ex.getMessage());
            throw new ApiException(
                    "Could not verify CAPTCHA. Try again in a moment.",
                    HttpStatus.SERVICE_UNAVAILABLE.value());
        }

        if (result == null || !result.success()) {
            log.warn("reCAPTCHA rejected signup errorCodes={}",
                    result != null ? result.errorCodes() : "null-response");
            throw new ApiException(
                    "CAPTCHA verification failed. Please try again.",
                    HttpStatus.BAD_REQUEST.value());
        }
        log.debug("reCAPTCHA verified hostname={}", result.hostname());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SiteVerifyResponse(
            boolean success,
            String hostname,
            @JsonProperty("challenge_ts") String challengeTs,
            @JsonProperty("error-codes") java.util.List<String> errorCodes
    ) {}
}
