package com.ecommerce.minishopee.payment.dto.response;

import com.ecommerce.minishopee.payment.enums.PaymentSessionStatus;
import com.ecommerce.minishopee.payment.enums.PaymentProvider;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PaymentResponse {

    private String paymentCode;

    private PaymentProvider provider;

    private PaymentSessionStatus status;

    private BigDecimal amount;

    private String bankCode;

    private String accountNumber;

    private String accountName;

    private String qrUrl;

    private LocalDateTime expiresAt;

    private LocalDateTime paidAt;

    private List<PaymentOrderResponse> orders;
}
