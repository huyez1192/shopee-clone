package com.ecommerce.minishopee.order.admin.dto.request;

import com.ecommerce.minishopee.order.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateOrderStatusRequest {

    @NotNull(message = "Trạng thái đơn hàng không được để trống.")
    private OrderStatus status;
}