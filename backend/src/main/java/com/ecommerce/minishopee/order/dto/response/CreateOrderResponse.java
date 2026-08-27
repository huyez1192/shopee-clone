package com.ecommerce.minishopee.order.dto.response;

import com.ecommerce.minishopee.payment.dto.response.PaymentResponse;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CreateOrderResponse {

    private List<OrderResponse> orders;

    private Integer orderCount;

    private BigDecimal merchandiseTotal;

    private BigDecimal shippingTotal;

    private BigDecimal grandTotal;

    private PaymentResponse payment;
}