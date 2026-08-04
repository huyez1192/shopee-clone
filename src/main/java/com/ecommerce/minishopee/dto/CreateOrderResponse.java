package com.ecommerce.minishopee.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class CreateOrderResponse {

    private List<OrderResponse> orders;

    private Integer orderCount;

    private BigDecimal merchandiseTotal;

    private BigDecimal shippingTotal;

    private BigDecimal grandTotal;
}