package com.ecommerce.minishopee.payment.service;

import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.order.enums.PaymentStatus;
import com.ecommerce.minishopee.order.repository.OrderRepository;
import com.ecommerce.minishopee.payment.dto.request.SePayWebhookRequest;
import com.ecommerce.minishopee.payment.entity.Payment;
import com.ecommerce.minishopee.payment.enums.PaymentSessionStatus;
import com.ecommerce.minishopee.payment.repository.PaymentRepository;
import com.ecommerce.minishopee.payment.repository.SePayTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SePayWebhookServiceTests {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SePayTransactionRepository transactionRepository;
    @Mock
    private OrderRepository orderRepository;

    @Test
    void paidWebhookMarksEveryOrderInPaymentGroupPaid() {
        Payment payment = new Payment();
        payment.setPaymentCode("MS123");
        payment.setAccountNumber("123456");
        payment.setAmount(BigDecimal.valueOf(500_000));
        payment.setStatus(PaymentSessionStatus.PENDING);

        Order first = unpaidOrder();
        Order second = unpaidOrder();
        Order third = unpaidOrder();
        payment.addOrder(first);
        payment.addOrder(second);
        payment.addOrder(third);

        SePayWebhookRequest request = new SePayWebhookRequest();
        request.setId(99L);
        request.setTransferType("in");
        request.setCode(payment.getPaymentCode());
        request.setAccountNumber(payment.getAccountNumber());
        request.setTransferAmount(payment.getAmount());

        when(transactionRepository.existsBySePayTransactionId(request.getId())).thenReturn(false);
        when(paymentRepository.findByPaymentCodeForUpdate(payment.getPaymentCode()))
                .thenReturn(Optional.of(payment));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SePayWebhookService service = new SePayWebhookService(
                paymentRepository,
                transactionRepository,
                orderRepository
        );

        service.process(request, "{}");

        assertEquals(PaymentSessionStatus.PAID, payment.getStatus());
        assertEquals(PaymentStatus.PAID, first.getPaymentStatus());
        assertEquals(PaymentStatus.PAID, second.getPaymentStatus());
        assertEquals(PaymentStatus.PAID, third.getPaymentStatus());
        verify(orderRepository).saveAll(payment.getOrders());
    }

    private Order unpaidOrder() {
        Order order = new Order();
        order.setPaymentStatus(PaymentStatus.UNPAID);
        return order;
    }
}
