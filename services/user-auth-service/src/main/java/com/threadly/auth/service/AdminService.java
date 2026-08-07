package com.threadly.auth.service;

import com.threadly.auth.dto.AssignRolesRequest;
import com.threadly.auth.dto.RoleResponse;
import com.threadly.auth.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminService {

    Page<UserResponse> listUsers(String q, Pageable pageable);

    UserResponse assignRoles(Long userId, AssignRolesRequest request, Long actorId, String actorUsername);

    void softDeleteUser(Long userId, Long actorId, String actorUsername);

    List<RoleResponse> listRoles();
}
