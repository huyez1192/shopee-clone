package com.ecommerce.minishopee.order.seller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerCancelOrderRequest {

    // Seller hủy đơn phải có lý do, vì người mua cần biết nguyên nhân.
    @NotBlank(message = "Lý do hủy đơn không được để trống.")
    @Size(max = 500, message = "Lý do hủy đơn không được vượt quá 500 ký tự.")
    private String reason;
}