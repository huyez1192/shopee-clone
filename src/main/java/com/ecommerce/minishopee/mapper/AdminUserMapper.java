package com.ecommerce.minishopee.mapper;

import com.ecommerce.minishopee.dto.AdminUserResponse;
import com.ecommerce.minishopee.entity.AppUser;
import com.ecommerce.minishopee.entity.Shop;

public class AdminUserMapper {

    private AdminUserMapper() {
    }

    public static AdminUserResponse toResponse(AppUser user, Shop shop) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .lockedReason(user.getLockedReason())
                .lockedAt(user.getLockedAt())
                .lockedByAdminId(user.getLockedByAdminId())
                .shopId(shop == null ? null : shop.getId())
                .shopName(shop == null ? null : shop.getName())
                .shopStatus(shop == null ? null : shop.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}