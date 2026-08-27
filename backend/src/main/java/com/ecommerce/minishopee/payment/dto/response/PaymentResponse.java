package com.ecommerce.minishopee.payment.dto.response;

import com.ecommerce.minishopee.payment.enums.PaymentSessionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentResponse {

    private String paymentCode;

    private PaymentSessionStatus status;

    private BigDecimal amount;

    private String bankCode;

    private String accountNumber;

    private String accountName;

    private String qrUrl;

    private LocalDateTime expiresAt;
}