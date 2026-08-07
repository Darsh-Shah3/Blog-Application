package com.threadly.auth.controller;

import com.threadly.auth.dto.AssignRolesRequest;
import com.threadly.auth.dto.RoleResponse;
import com.threadly.auth.dto.UserResponse;
import com.threadly.auth.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Platform administration — requires ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> listUsers(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminService.listUsers(q, pageable));
    }

    @PutMapping("/users/{id}/roles")
    public ResponseEntity<UserResponse> assignRoles(
            Authentication authentication,
            @RequestHeader(value = "X-Username", required = false) String username,
            @PathVariable Long id,
            @Valid @RequestBody AssignRolesRequest request) {
        Long actorId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(adminService.assignRoles(id, request, actorId, username));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> softDeleteUser(
            Authentication authentication,
            @RequestHeader(value = "X-Username", required = false) String username,
            @PathVariable Long id) {
        Long actorId = Long.parseLong(authentication.getName());
        adminService.softDeleteUser(id, actorId, username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> listRoles() {
        return ResponseEntity.ok(adminService.listRoles());
    }
}
