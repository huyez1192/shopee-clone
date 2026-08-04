package com.ecommerce.minishopee.dto;

import com.ecommerce.minishopee.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateOrderStatusRequest {

    @NotNull(message = "Trạng thái đơn hàng không được để trống.")
    private OrderStatus status;
}