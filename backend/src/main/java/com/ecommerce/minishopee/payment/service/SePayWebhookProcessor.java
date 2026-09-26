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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SePayWebhookProcessor {

    private static final Logger log = LoggerFactory.getLogger(SePayWebhookProcessor.class);
    private static final Pattern PAYMENT_CODE_PATTERN =
            Pattern.compile("(?i)(?<![A-Z0-9])MS[A-Z0-9]{3,46}(?![A-Z0-9])");

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
        if (!"in".equalsIgnoreCase(request.getTransferType())) {
            return null;
        }

        for (String candidate : paymentCodeCandidates(request)) {
            Optional<Payment> payment = paymentRepository.findByPaymentCodeForUpdate(candidate);
            if (payment.isPresent()) {
                return payment.get();
            }
        }

        return null;
    }

    private Set<String> paymentCodeCandidates(SePayWebhookRequest request) {
        Set<String> candidates = new LinkedHashSet<>();
        addExactCandidate(candidates, request.getCode());
        addEmbeddedCandidates(candidates, request.getContent());
        addEmbeddedCandidates(candidates, request.getDescription());
        return candidates;
    }

    private void addExactCandidate(Set<String> candidates, String value) {
        if (value != null && !value.isBlank()) {
            String candidate = value.trim();
            candidates.add(candidate);

            String uppercaseCandidate = candidate.toUpperCase(Locale.ROOT);
            if (!uppercaseCandidate.equals(candidate)) {
                candidates.add(uppercaseCandidate);
            }
        }
    }

    private void addEmbeddedCandidates(Set<String> candidates, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        Matcher matcher = PAYMENT_CODE_PATTERN.matcher(value);
        while (matcher.find()) {
            candidates.add(matcher.group().toUpperCase(Locale.ROOT));
        }
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
