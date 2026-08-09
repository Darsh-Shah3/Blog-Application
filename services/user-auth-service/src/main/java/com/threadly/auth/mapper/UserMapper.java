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
        java.util.UUID avatarId = user.getAvatarMediaId();
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .bio(user.getBio())
                .avatarMediaId(avatarId)
                .avatarUrl(avatarId != null ? "/api/v1/media/" + avatarId + "/content" : null)
                .karma(user.getKarma())
                .roles(roles)
                .permissions(RbacCatalog.permissionNamesForRoles(roles))
                .createdAt(user.getCreatedAt())
                .createdBy(user.getCreatedBy())
                .updatedBy(user.getUpdatedBy())
                .deleted(false)
                .build();
    }

    /** Public card for soft-deleted accounts (posts/chat still reference the username snapshot). */
    public UserResponse toDeletedPublicCard(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(null)
                .displayName("Deleted account")
                .bio(null)
                .avatarMediaId(null)
                .avatarUrl(null)
                .karma(0L)
                .roles(java.util.Set.of())
                .permissions(java.util.Set.of())
                .createdAt(user.getCreatedAt())
                .createdBy(user.getCreatedBy())
                .updatedBy(user.getUpdatedBy())
                .deleted(true)
                .build();
    }
}
