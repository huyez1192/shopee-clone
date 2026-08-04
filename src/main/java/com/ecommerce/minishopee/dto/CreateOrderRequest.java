package com.ecommerce.minishopee.dto;

import com.ecommerce.minishopee.entity.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateOrderRequest {

    @NotNull(message = "addressId không được để trống.")
    private Long addressId;

    @NotEmpty(message = "Phải chọn ít nhất một CartItem để đặt hàng.")
    private List<@NotNull Long> cartItemIds;

    @NotNull(message = "Phương thức thanh toán không được để trống.")
    private PaymentMethod paymentMethod;
}