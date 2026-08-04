package com.ecommerce.minishopee.dto;

import com.ecommerce.minishopee.security.Role;
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
