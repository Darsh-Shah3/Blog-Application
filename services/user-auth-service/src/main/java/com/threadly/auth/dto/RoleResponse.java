package com.threadly.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class RoleResponse {
    private String name;
    private String displayName;
    private String description;
    private Set<String> permissions;
}
