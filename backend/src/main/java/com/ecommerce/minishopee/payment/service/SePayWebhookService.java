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

import java.time.LocalDateTime;

@Service
public class SePayWebhookService {

    private final PaymentRepository paymentRepository;

    private final SePayTransactionRepository sePayTransactionRepository;

    private final OrderRepository orderRepository;

    public SePayWebhookService(
            PaymentRepository paymentRepository,
            SePayTransactionRepository sePayTransactionRepository,
            OrderRepository orderRepository
    ) {
        this.paymentRepository = paymentRepository;

        this.sePayTransactionRepository = sePayTransactionRepository;

        this.orderRepository = orderRepository;
    }

    @Transactional
    public void process(
            SePayWebhookRequest request,
            String rawPayload
    ) {
        if (request.getId() == null) {
            throw new BadRequestException(
                    "Webhook SePay không có transaction id."
            );
        }

        /*
         * SePay có thể retry cùng một webhook.
         */
        if (sePayTransactionRepository
                .existsBySePayTransactionId(request.getId())) {
            return;
        }

        /*
         * Shopee chỉ quan tâm tiền vào.
         */
        if (!"in".equalsIgnoreCase(
                    request.getTransferType())
        ) {
            saveTransaction(
                    request,
                    null,
                    rawPayload
            );

            return;
        }

        /*
         * Không nhận diện được payment code
         * thì không cập nhật Payment nào.
         */
        if (request.getCode() == null || request.getCode().isBlank()) {
            saveTransaction(
                    request,
                    null,
                    rawPayload
            );

            return;
        }

        Payment payment =
                paymentRepository
                        .findByPaymentCodeForUpdate(
                                request.getCode()
                        )
                        .orElse(null);

        /*
         * Có thể đây là giao dịch không thuộc
         * Mini Shopee.
         */
        if (payment == null) {
            saveTransaction(
                    request,
                    null,
                    rawPayload
            );

            return;
        }

        /*
         * Lưu giao dịch trước khi xử lý payment.
         */
        saveTransaction(
                request,
                payment,
                rawPayload
        );

        /*
         * Payment đã xử lý rồi thì không làm lại.
         */
        if (payment.getStatus() == PaymentSessionStatus.PAID) {
            return;
        }

        if (payment.getStatus() != PaymentSessionStatus.PENDING) {
            return;
        }

        /*
         * Kiểm tra tài khoản nhận tiền.
         */
        if (request.getAccountNumber() == null
                        || !request.getAccountNumber()
                        .equals(payment.getAccountNumber())
        ) {
            throw new BadRequestException("Tài khoản nhận tiền không khớp Payment.");
        }

        /*
         * Kiểm tra số tiền phải chính xác.
         */
        if (request.getTransferAmount() == null
                        || request.getTransferAmount()
                        .compareTo(payment.getAmount()) != 0
        ) {
            throw new BadRequestException("Số tiền giao dịch không khớp Payment.");
        }

        /*
         * Payment thành công.
         */
        payment.setStatus(PaymentSessionStatus.PAID);

        payment.setPaidAt(LocalDateTime.now());

        /*
         * Tất cả Order thuộc lần checkout này
         * đều được đánh dấu đã thanh toán.
         *
         * Không thay Order.status.
         *
         * Order.status vẫn PENDING để Seller
         * xác nhận sau.
         */
        for (Order order : payment.getOrders()) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }

        paymentRepository.save(payment);

        orderRepository.saveAll(payment.getOrders());
    }

    private void saveTransaction(
            SePayWebhookRequest request,
            Payment payment,
            String rawPayload
    ) {
        SePayTransaction transaction = new SePayTransaction();

        transaction.setSePayTransactionId(request.getId());

        transaction.setPayment(payment);

        transaction.setGateway(request.getGateway());

        transaction.setAccountNumber(request.getAccountNumber());

        transaction.setTransferAmount(request.getTransferAmount());

        transaction.setReferenceCode(request.getReferenceCode());

        transaction.setRawPayload(rawPayload);

        sePayTransactionRepository.save(transaction);
    }
}