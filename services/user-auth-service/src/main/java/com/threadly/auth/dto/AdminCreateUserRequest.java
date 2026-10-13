package com.threadly.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

/**
 * Admin-provisioned account. Roles are the platform access model (RBAC);
 * permissions are derived from assigned roles via {@code RbacCatalog}.
 */
@Data
public class AdminCreateUserRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @Size(max = 100)
    private String displayName;

    /**
     * Optional initial roles (e.g. ROLE_USER, ROLE_MODERATOR, ROLE_ADMIN).
     * When empty/null, defaults to ROLE_USER. ROLE_USER is always included.
     */
    private Set<String> roles;
}
