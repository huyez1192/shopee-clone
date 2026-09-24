package com.ecommerce.minishopee.payment.service;

import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.order.enums.PaymentStatus;
import com.ecommerce.minishopee.order.repository.OrderRepository;
import com.ecommerce.minishopee.payment.dto.request.SePayWebhookRequest;
import com.ecommerce.minishopee.payment.entity.Payment;
import com.ecommerce.minishopee.payment.entity.SePayTransaction;
import com.ecommerce.minishopee.payment.enums.PaymentSessionStatus;
import com.ecommerce.minishopee.payment.repository.PaymentRepository;
import com.ecommerce.minishopee.payment.repository.SePayTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SePayWebhookProcessor {

    private static final Logger log = LoggerFactory.getLogger(SePayWebhookProcessor.class);

    private final PaymentRepository paymentRepository;
    private final SePayTransactionRepository transactionRepository;
    private final OrderRepository orderRepository;

    public SePayWebhookProcessor(
            PaymentRepository paymentRepository,
            SePayTransactionRepository transactionRepository,
            OrderRepository orderRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public void processFirstDelivery(SePayWebhookRequest request, String rawPayload) {
        Payment payment = findPaymentForUpdate(request);

        SePayTransaction transaction = new SePayTransaction();
        transaction.setSePayTransactionId(request.getId());
        transaction.setPayment(payment);
        transaction.setGateway(request.getGateway());
        transaction.setAccountNumber(request.getAccountNumber());
        transaction.setTransferAmount(request.getTransferAmount());
        transaction.setReferenceCode(request.getReferenceCode());
        transaction.setRawPayload(rawPayload);

        // Flush tại đây để unique collision xảy ra bên trong transaction này.
        transactionRepository.saveAndFlush(transaction);

        if (payment == null) {
            return;
        }

        if (payment.getStatus() == PaymentSessionStatus.EXPIRED) {
            log.warn(
                    "Late SePay payment requires manual reconciliation: transactionId={}, paymentCode={}, amount={}, paymentStatus={}",
                    request.getId(), payment.getPaymentCode(), request.getTransferAmount(), payment.getStatus()
            );
            return;
        }

        if (payment.getStatus() != PaymentSessionStatus.PENDING) {
            return;
        }

        validatePaymentDetails(request, payment);

        List<Order> lockedOrders = orderRepository
                .findAllByPaymentIdForUpdateOrdered(payment.getId());

        payment.setStatus(PaymentSessionStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());

        for (Order order : lockedOrders) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }

        paymentRepository.save(payment);
        orderRepository.saveAll(lockedOrders);
    }

    private Payment findPaymentForUpdate(SePayWebhookRequest request) {
        if (!"in".equalsIgnoreCase(request.getTransferType())
                || request.getCode() == null
                || request.getCode().isBlank()) {
            return null;
        }

        return paymentRepository.findByPaymentCodeForUpdate(request.getCode()).orElse(null);
    }

    private void validatePaymentDetails(SePayWebhookRequest request, Payment payment) {
        if (request.getAccountNumber() == null
                || !request.getAccountNumber().equals(payment.getAccountNumber())) {
            throw new BadRequestException("Tài khoản nhận tiền không khớp Payment.");
        }

        if (request.getTransferAmount() == null
                || request.getTransferAmount().compareTo(payment.getAmount()) != 0) {
            throw new BadRequestException("Số tiền giao dịch không khớp Payment.");
        }
    }
}
