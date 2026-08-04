package com.ecommerce.minishopee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopRegisterRequest {

    @NotBlank(message = "Tên shop không được để trống.")
    private String name;

    private String description;

    private String logoUrl;

    @NotBlank(message = "Địa chỉ shop không được để trống.")
    private String address;

    @NotBlank(message = "Số điện thoại liên hệ không được để trống.")
    private String contactPhone;
}