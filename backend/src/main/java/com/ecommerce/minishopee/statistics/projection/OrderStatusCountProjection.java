package com.ecommerce.minishopee.statistics.projection;

import com.ecommerce.minishopee.order.enums.OrderStatus;

public interface OrderStatusCountProjection {
    OrderStatus getStatus();
    Long getOrderCount();
}
