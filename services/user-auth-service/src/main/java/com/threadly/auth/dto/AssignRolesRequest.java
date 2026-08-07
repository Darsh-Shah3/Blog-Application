package com.threadly.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class AssignRolesRequest {
    /** Full replacement set of role names, e.g. ROLE_USER, ROLE_MODERATOR. */
    @NotEmpty
    private Set<String> roles;
}
