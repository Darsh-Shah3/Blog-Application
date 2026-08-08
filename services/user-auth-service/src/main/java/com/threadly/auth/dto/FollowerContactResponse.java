package com.threadly.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class FollowerContactResponse {
    private UUID id;
    private String username;
    private String email;
}
