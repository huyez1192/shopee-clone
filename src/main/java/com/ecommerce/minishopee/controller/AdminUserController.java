package com.ecommerce.minishopee.controller;

import com.ecommerce.minishopee.dto.AdminLockUserRequest;
import com.ecommerce.minishopee.dto.AdminUserResponse;
import com.ecommerce.minishopee.entity.AppUser;
import com.ecommerce.minishopee.entity.UserStatus;
import com.ecommerce.minishopee.security.Role;
import com.ecommerce.minishopee.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /*
     * Ví dụ:
     *
     * GET /api/admin/users
     * GET /api/admin/users?role=SELLER
     * GET /api/admin/users?status=LOCKED
     * GET /api/admin/users?keyword=gmail
     */
    @GetMapping
    public List<AdminUserResponse> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status
    ) {
        return adminUserService.getUsers(keyword, role, status);
    }

    // Admin xem chi tiết User.
    @GetMapping("/{id}")
    public AdminUserResponse getUserById(@PathVariable Long id) {
        return adminUserService.getUserById(id);
    }

    // Admin khóa User.
    @PatchMapping("/{id}/lock")
    public AdminUserResponse lockUser(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentAdmin,
            @Valid @RequestBody AdminLockUserRequest request
    ) {
        return adminUserService.lockUser(id, currentAdmin, request);
    }

    // Admin mở khóa User.
    @PatchMapping("/{id}/unlock")
    public AdminUserResponse unlockUser(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentAdmin
    ) {
        return adminUserService.unlockUser(id, currentAdmin);
    }
}