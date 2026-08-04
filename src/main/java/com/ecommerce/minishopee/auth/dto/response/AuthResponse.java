package com.ecommerce.minishopee.auth.dto.response;

import com.ecommerce.minishopee.user.enums.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthResponse {

    private String token;

    private String tokenType;

    private String email;

    private Role role;
}
