package com.ecommerce.minishopee.user.dto.response;

import com.ecommerce.minishopee.user.enums.UserStatus;
import com.ecommerce.minishopee.user.enums.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class UserResponse {
    private Long id;

    private String fullName;

    private String email;

    private String phone;

    private String avatarUrl;

    private Role role;

    private UserStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
