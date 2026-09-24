package com.ecommerce.minishopee.payment.service;

import com.ecommerce.minishopee.payment.config.SePayProperties;
import com.ecommerce.minishopee.payment.dto.response.PaymentResponse;
import com.ecommerce.minishopee.payment.dto.response.PaymentOrderResponse;
import com.ecommerce.minishopee.payment.entity.Payment;
import com.ecommerce.minishopee.payment.enums.PaymentProvider;
import com.ecommerce.minishopee.payment.enums.PaymentSessionStatus;
import com.ecommerce.minishopee.payment.repository.PaymentRepository;
import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.payment.dto.response.PaymentStatusResponse;
import com.ecommerce.minishopee.user.entity.AppUser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentService {

    private static final DateTimeFormatter PAYMENT_CODE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final long SEPAY_EXPIRE_MINUTES = 15;

    private final PaymentRepository paymentRepository;
    private final SePayProperties sePayProperties;
    private final SePayQrService sePayQrService;

    public PaymentService(
            PaymentRepository paymentRepository,
            SePayProperties sePayProperties,
            SePayQrService sePayQrService
    ) {
        this.paymentRepository = paymentRepository;
        this.sePayProperties = sePayProperties;
        this.sePayQrService = sePayQrService;
    }

    public Payment createSePayPayment(
            AppUser user,
            BigDecimal amount
    ) {
        LocalDateTime now = LocalDateTime.now();

        Payment payment = new Payment();

        payment.setPaymentCode(
                generateUniquePaymentCode()
        );

        payment.setUser(user);

        payment.setProvider(
                PaymentProvider.SEPAY
        );

        payment.setAmount(amount);

        payment.setStatus(
                PaymentSessionStatus.PENDING
        );

        /*
         * Lưu snapshot tài khoản nhận tiền
         * vào chính Payment.
         */
        payment.setBankCode(
                sePayProperties.getBankCode()
        );

        payment.setAccountNumber(
                sePayProperties.getAccountNumber()
        );

        payment.setExpiresAt(
                now.plusMinutes(
                        SEPAY_EXPIRE_MINUTES
                )
        );

        return paymentRepository.save(payment);
    }

    public PaymentResponse toResponse(
            Payment payment
    ) {
        if (payment == null) {
            return null;
        }

        return PaymentResponse.builder()
                .paymentCode(payment.getPaymentCode())
                .provider(payment.getProvider())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .bankCode(payment.getBankCode())
                .accountNumber(payment.getAccountNumber())
                .accountName(sePayProperties.getAccountName())
                .qrUrl(sePayQrService.generateQrUrl(payment))
                .expiresAt(payment.getExpiresAt())
                .paidAt(payment.getPaidAt())
                .orders(payment.getOrders().stream()
                        .map(order -> PaymentOrderResponse.builder()
                                .id(order.getId())
                                .orderCode(order.getOrderCode())
                                .build())
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(AppUser currentUser, String paymentCode) {
        Payment payment = paymentRepository
                .findByPaymentCodeAndUserId(paymentCode, currentUser.getId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy Payment."));

        return toResponse(payment);
    }

    public PaymentStatusResponse getPaymentStatus(
            AppUser currentUser,
            String paymentCode
    ) {
        Payment payment =
                paymentRepository
                        .findByPaymentCodeAndUserId(
                                paymentCode,
                                currentUser.getId()
                        )
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Không tìm thấy Payment."
                                )
                        );

        return PaymentStatusResponse.builder()
                .paymentCode(
                        payment.getPaymentCode()
                )
                .status(
                        payment.getStatus()
                )
                .amount(
                        payment.getAmount()
                )
                .expiresAt(
                        payment.getExpiresAt()
                )
                .paidAt(
                        payment.getPaidAt()
                )
                .build();
    }

    private String generateUniquePaymentCode() {
        String timestamp = LocalDateTime.now().format(PAYMENT_CODE_TIME_FORMAT);
        String randomPart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase(Locale.ROOT);

        // Giữ prefix/format không dấu; tổng chiều dài 48, trong giới hạn cột 50.
        return "MS" + timestamp + randomPart;
    }
}
