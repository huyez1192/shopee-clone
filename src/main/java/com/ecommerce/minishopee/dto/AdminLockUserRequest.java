package com.ecommerce.minishopee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminLockUserRequest {

    @NotBlank(message = "Lý do khóa tài khoản không được để trống.")
    @Size(max = 500, message = "Lý do khóa không được vượt quá 500 ký tự.")
    private String reason;
}