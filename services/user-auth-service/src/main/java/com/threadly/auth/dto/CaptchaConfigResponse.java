package com.threadly.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaptchaConfigResponse {
    /** When true, signup must include a valid captchaToken. */
    private boolean enabled;
    /** e.g. recaptcha-v2 */
    private String provider;
    /** Public site key for the frontend widget (never the secret). */
    private String siteKey;
}
