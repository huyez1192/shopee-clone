package com.ecommerce.minishopee.payment.controller;

import com.ecommerce.minishopee.payment.dto.response.PaymentStatusResponse;
import com.ecommerce.minishopee.payment.service.PaymentService;
import com.ecommerce.minishopee.user.entity.AppUser;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(
            PaymentService paymentService
    ) {
        this.paymentService = paymentService;
    }

    /*
     * Buyer kiểm tra trạng thái thanh toán SePay.
     *
     * Ví dụ:
     *
     * GET /api/payments/MSP20260813144014ACC013/status
     *
     * Chỉ user sở hữu Payment mới được xem.
     */
    @GetMapping("/{paymentCode}/status")
    public PaymentStatusResponse getPaymentStatus(
            @PathVariable String paymentCode,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        return paymentService.getPaymentStatus(
                currentUser,
                paymentCode
        );
    }
}