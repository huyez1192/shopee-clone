package com.ecommerce.minishopee.payment.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentOrderResponse {
    private Long id;
    private String orderCode;
}
