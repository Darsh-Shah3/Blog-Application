package com.threadly.auth.service;

import com.threadly.auth.dto.AssignRolesRequest;
import com.threadly.auth.dto.RoleResponse;
import com.threadly.auth.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AdminService {

    Page<UserResponse> listUsers(String q, Pageable pageable);

    UserResponse assignRoles(UUID userId, AssignRolesRequest request, UUID actorId, String actorUsername);

    void softDeleteUser(UUID userId, UUID actorId, String actorUsername);

    List<RoleResponse> listRoles();
}
