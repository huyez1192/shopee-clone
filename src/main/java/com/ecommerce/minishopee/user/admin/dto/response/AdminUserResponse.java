package com.ecommerce.minishopee.user.admin.dto.response;

import com.ecommerce.minishopee.shop.enums.ShopStatus;
import com.ecommerce.minishopee.user.enums.UserStatus;
import com.ecommerce.minishopee.user.enums.Role;
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