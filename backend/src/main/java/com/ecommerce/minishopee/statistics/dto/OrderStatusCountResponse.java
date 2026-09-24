package com.ecommerce.minishopee.statistics.dto;

import com.ecommerce.minishopee.order.enums.OrderStatus;

public record OrderStatusCountResponse(OrderStatus status, long count) {
}
