package com.ecommerce.minishopee.order.checkout.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CheckoutPreviewRequest {

    @NotNull(message = "addressId không được để trống.")
    private Long addressId;

    @NotEmpty(message = "Phải chọn ít nhất một CartItem để checkout.")
    private List<@NotNull Long> cartItemIds;
}