package com.ecommerce.minishopee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest {

    @NotBlank(message = "Tên người nhận không được để trống.")
    private String recipientName;

    @NotBlank(message = "Số điện thoại người nhận không được để trống.")
    private String recipientPhone;

    @NotBlank(message = "Tỉnh/thành phố không được để trống.")
    private String province;

    @NotBlank(message = "Phường/xã không được để trống.")
    private String ward;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống.")
    private String detailAddress;

    private String note;

    private boolean defaultAddress;
}
