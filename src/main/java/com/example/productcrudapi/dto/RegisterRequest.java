package com.example.productcrudapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Email không được bỏ trống.")
    @Email(message = "Email không hợp lệ.")
    private String email;

    @NotBlank(message = "Password không được bỏ trống.")
    @Size(min=6, message = "Password phải chứa ít nhất 6 ký tự.")
    private String password;
}
