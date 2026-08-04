package com.ecommerce.minishopee.dto;

import com.ecommerce.minishopee.entity.ShopStatus;
import com.ecommerce.minishopee.entity.UserStatus;
import com.ecommerce.minishopee.security.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AdminUserResponse {

    private Long id;

    private String fullName;

    private String email;

    private String phone;

    private String avatarUrl;

    private Role role;

    private UserStatus status;

    private String lockedReason;

    private LocalDateTime lockedAt;

    private Long lockedByAdminId;

    // Các field shop sẽ null nếu user chưa đăng ký shop.
    private Long shopId;

    private String shopName;

    private ShopStatus shopStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}