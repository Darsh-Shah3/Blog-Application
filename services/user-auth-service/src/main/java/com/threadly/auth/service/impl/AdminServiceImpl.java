package com.threadly.auth.service.impl;

import com.threadly.auth.dto.AssignRolesRequest;
import com.threadly.auth.dto.RoleResponse;
import com.threadly.auth.dto.UserResponse;
import com.threadly.auth.entity.Role;
import com.threadly.auth.entity.User;
import com.threadly.auth.exception.ApiException;
import com.threadly.auth.mapper.UserMapper;
import com.threadly.auth.rbac.AppRole;
import com.threadly.auth.rbac.RbacCatalog;
import com.threadly.auth.repository.RoleRepository;
import com.threadly.auth.repository.UserRepository;
import com.threadly.auth.service.AdminService;
import com.threadly.auth.util.AuditActors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return userRepository.findAllActive(pageable).map(userMapper::toResponse);
        }
        String term = q.trim().toLowerCase(Locale.ROOT);
        return userRepository.searchActive(term, pageable).map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public UserResponse assignRoles(Long userId, AssignRolesRequest request, Long actorId, String actorUsername) {
        User user = findActiveUser(userId);

        Set<String> wanted = request.getRoles().stream()
                .map(r -> r.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));

        if (wanted.isEmpty()) {
            throw new ApiException("At least one role is required", HttpStatus.BAD_REQUEST.value());
        }
        for (String name : wanted) {
            if (AppRole.fromName(name).isEmpty()) {
                throw new ApiException("Unknown role: " + name, HttpStatus.BAD_REQUEST.value());
            }
        }
        if (wanted.contains(AppRole.ROLE_ADMIN.name()) || wanted.contains(AppRole.ROLE_MODERATOR.name())) {
            wanted.add(AppRole.ROLE_USER.name());
        }

        boolean removingAdmin = user.getRoles().stream().anyMatch(r -> AppRole.ROLE_ADMIN.name().equals(r.getName()))
                && !wanted.contains(AppRole.ROLE_ADMIN.name());
        if (removingAdmin) {
            long adminCount = userRepository.countActiveByRoleName(AppRole.ROLE_ADMIN.name());
            if (adminCount <= 1) {
                throw new ApiException("Cannot remove the last platform admin", HttpStatus.BAD_REQUEST.value());
            }
        }

        Set<Role> roles = new HashSet<>();
        for (String name : wanted) {
            Role role = roleRepository.findByName(name)
                    .orElseThrow(() -> new ApiException("Role missing in database: " + name, 500));
            roles.add(role);
        }
        user.getRoles().clear();
        user.getRoles().addAll(roles);
        user.setUpdatedBy(AuditActors.resolve(actorUsername, actorId));
        user = userRepository.save(user);
        log.info("Roles updated userId={} actorId={} actor={} roles={}", userId, actorId, user.getUpdatedBy(), wanted);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void softDeleteUser(Long userId, Long actorId, String actorUsername) {
        User user = findActiveUser(userId);
        if (userId.equals(actorId)) {
            throw new ApiException("Use account delete for yourself", HttpStatus.BAD_REQUEST.value());
        }
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> AppRole.ROLE_ADMIN.name().equals(r.getName()));
        if (isAdmin) {
            long adminCount = userRepository.countActiveByRoleName(AppRole.ROLE_ADMIN.name());
            if (adminCount <= 1) {
                throw new ApiException("Cannot soft-delete the last platform admin", HttpStatus.BAD_REQUEST.value());
            }
        }
        String actor = AuditActors.resolve(actorUsername, actorId);
        user.setDeletedAt(Instant.now());
        user.setUpdatedBy(actor);
        userRepository.save(user);
        log.info("User soft-deleted id={} username={} by actor={}", userId, user.getUsername(), actor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        return roleRepository.findAll().stream()
                .map(this::toRoleResponse)
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .toList();
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND.value()));
        if (user.isDeleted()) {
            throw new ApiException("User not found", HttpStatus.NOT_FOUND.value());
        }
        return user;
    }

    private RoleResponse toRoleResponse(Role role) {
        AppRole app = AppRole.fromName(role.getName()).orElse(null);
        String display = app != null ? app.getDisplayName() : role.getName();
        String description = role.getDescription() != null ? role.getDescription()
                : (app != null ? app.getDescription() : "");
        Set<String> permissions = app != null
                ? RbacCatalog.permissionNamesForRoles(List.of(app.name()))
                : Set.of();
        return RoleResponse.builder()
                .name(role.getName())
                .displayName(display)
                .description(description)
                .permissions(permissions)
                .build();
    }
}
