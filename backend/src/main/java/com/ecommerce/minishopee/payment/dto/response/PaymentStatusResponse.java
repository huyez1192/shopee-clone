package com.ecommerce.minishopee.payment.dto.response;

import com.ecommerce.minishopee.payment.enums.PaymentSessionStatus;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentStatusResponse {

    private String paymentCode;

    private PaymentSessionStatus status;

    private BigDecimal amount;

    private LocalDateTime expiresAt;

    private LocalDateTime paidAt;
}