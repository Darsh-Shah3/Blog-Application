package com.threadly.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthStartResponse {
    private String provider;
    private boolean configured;
    private String authorizationUrl;
    private String message;
}
