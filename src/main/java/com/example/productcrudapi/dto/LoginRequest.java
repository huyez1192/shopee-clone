package com.example.productcrudapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Email không được bỏ trống.")
    @Email(message = "Email không hợp lệ.")
    private String email;

    @NotBlank(message = "Email không được bỏ trống.")
    private String password;
}
