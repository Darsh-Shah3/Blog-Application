package com.threadly.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuthCallbackRequest {
    @NotBlank
    private String code;

    /** Opaque state from start (provider:nonce:returnTo). */
    private String state;
}
