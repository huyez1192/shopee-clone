package com.ecommerce.minishopee.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Họ tên không được để trống.")
    private String fullName;

    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Email không đúng định dạng.")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống.")
    private String phone;

    @NotBlank(message = "Password không được bỏ trống.")
    @Size(min=6, message = "Password phải có ít nhất 6 ký tự.")
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống.")
    private String confirmPassword;
}
