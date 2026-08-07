package com.threadly.auth.mapper;

import com.threadly.auth.dto.UserResponse;
import com.threadly.auth.entity.Role;
import com.threadly.auth.entity.User;
import com.threadly.auth.rbac.RbacCatalog;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Isolates entity &lt;-&gt; DTO mapping so services stay free of response construction noise.
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        Set<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .bio(user.getBio())
                .karma(user.getKarma())
                .roles(roles)
                .permissions(RbacCatalog.permissionNamesForRoles(roles))
                .createdAt(user.getCreatedAt())
                .createdBy(user.getCreatedBy())
                .updatedBy(user.getUpdatedBy())
                .build();
    }
}
