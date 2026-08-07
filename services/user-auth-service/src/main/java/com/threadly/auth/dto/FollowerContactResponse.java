package com.threadly.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FollowerContactResponse {
    private Long id;
    private String username;
    private String email;
}
