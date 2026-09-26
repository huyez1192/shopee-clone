package com.ecommerce.minishopee.payment.service;

import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.order.enums.OrderStatus;
import com.ecommerce.minishopee.order.enums.PaymentStatus;
import com.ecommerce.minishopee.order.repository.OrderRepository;
import com.ecommerce.minishopee.payment.dto.request.SePayWebhookRequest;
import com.ecommerce.minishopee.payment.entity.Payment;
import com.ecommerce.minishopee.payment.entity.SePayTransaction;
import com.ecommerce.minishopee.payment.enums.PaymentSessionStatus;
import com.ecommerce.minishopee.payment.repository.PaymentRepository;
import com.ecommerce.minishopee.payment.repository.SePayTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import com.ecommerce.minishopee.common.exception.BadRequestException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SePayWebhookServiceTests {

    @Mock private PaymentRepository paymentRepository;
    @Mock private SePayTransactionRepository transactionRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private SePayWebhookProcessor processor;

    private SePayWebhookService service;

    @BeforeEach
    void setUp() {
        service = new SePayWebhookService(processor, transactionRepository);
    }

    @Test
    void firstDeliveryAtomicallyMarksPaymentAndAllLockedOrdersPaid() {
        Payment payment = payment(PaymentSessionStatus.PENDING);
        List<Order> orders = List.of(unpaidOrder(), unpaidOrder(), unpaidOrder());
        SePayWebhookRequest request = request();
        when(paymentRepository.findByPaymentCodeForUpdate(request.getCode()))
                .thenReturn(Optional.of(payment));
        when(orderRepository.findAllByPaymentIdForUpdateOrdered(payment.getId()))
                .thenReturn(orders);
        when(transactionRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        new SePayWebhookProcessor(paymentRepository, transactionRepository, orderRepository)
                .processFirstDelivery(request, "{}");

        assertEquals(PaymentSessionStatus.PAID, payment.getStatus());
        orders.forEach(order -> assertEquals(PaymentStatus.PAID, order.getPaymentStatus()));
        verify(transactionRepository).saveAndFlush(any());
        verify(orderRepository).saveAll(orders);
    }

    @Test
    void paymentCodeInTransferContentMarksPaymentPaidWhenSePayCodeIsMissing() {
        Payment payment = payment(PaymentSessionStatus.PENDING);
        List<Order> orders = List.of(unpaidOrder());
        SePayWebhookRequest request = request();
        request.setCode(null);
        request.setContent("Thanh toan ms123 cho don hang");
        when(paymentRepository.findByPaymentCodeForUpdate("MS123"))
                .thenReturn(Optional.of(payment));
        when(orderRepository.findAllByPaymentIdForUpdateOrdered(payment.getId()))
                .thenReturn(orders);
        when(transactionRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        new SePayWebhookProcessor(paymentRepository, transactionRepository, orderRepository)
                .processFirstDelivery(request, "{}");

        assertEquals(PaymentSessionStatus.PAID, payment.getStatus());
        assertEquals(PaymentStatus.PAID, orders.getFirst().getPaymentStatus());
    }

    @Test
    void transferContentIsUsedWhenSePayCodeDoesNotIdentifyAPayment() {
        Payment payment = payment(PaymentSessionStatus.PENDING);
        SePayWebhookRequest request = request();
        request.setCode("BANK_REFERENCE");
        request.setDescription("IBFT MS123 THANH TOAN");
        when(paymentRepository.findByPaymentCodeForUpdate("BANK_REFERENCE"))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByPaymentCodeForUpdate("MS123"))
                .thenReturn(Optional.of(payment));
        when(orderRepository.findAllByPaymentIdForUpdateOrdered(payment.getId()))
                .thenReturn(List.of());
        when(transactionRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        new SePayWebhookProcessor(paymentRepository, transactionRepository, orderRepository)
                .processFirstDelivery(request, "{}");

        assertEquals(PaymentSessionStatus.PAID, payment.getStatus());
        verify(paymentRepository).findByPaymentCodeForUpdate("BANK_REFERENCE");
        verify(paymentRepository).findByPaymentCodeForUpdate("MS123");
    }

    @Test
    void exactConcurrentUniqueCollisionIsIdempotentSuccess() {
        SePayWebhookRequest request = request();
        String rawPayload = "{\"id\":99}";
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(processor).processFirstDelivery(request, rawPayload);
        when(transactionRepository.findBySePayTransactionId(request.getId()))
                .thenReturn(Optional.of(existingTransaction(request, rawPayload)));

        assertDoesNotThrow(() -> service.process(request, rawPayload));
    }

    @Test
    void nonDuplicateIntegrityViolationIsNotSwallowed() {
        SePayWebhookRequest request = request();
        DataIntegrityViolationException violation = new DataIntegrityViolationException("other constraint");
        doThrow(violation).when(processor).processFirstDelivery(request, "{}");
        when(transactionRepository.findBySePayTransactionId(request.getId()))
                .thenReturn(Optional.empty());

        assertSame(violation, assertThrows(DataIntegrityViolationException.class,
                () -> service.process(request, "{}")));
    }

    @Test
    void sameTransactionIdWithDifferentPayloadIsRejected() {
        SePayWebhookRequest request = request();
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(processor).processFirstDelivery(request, "changed");
        when(transactionRepository.findBySePayTransactionId(request.getId()))
                .thenReturn(Optional.of(existingTransaction(request, "original")));

        assertThrows(DataIntegrityViolationException.class,
                () -> service.process(request, "changed"));
    }

    @Test
    void webhookAfterExpirationDoesNotResurrectPaymentOrCancelledOrders() {
        Payment payment = payment(PaymentSessionStatus.EXPIRED);
        Order cancelled = unpaidOrder();
        cancelled.setStatus(OrderStatus.CANCELLED);
        SePayWebhookRequest request = request();
        when(paymentRepository.findByPaymentCodeForUpdate(request.getCode()))
                .thenReturn(Optional.of(payment));
        when(transactionRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        new SePayWebhookProcessor(paymentRepository, transactionRepository, orderRepository)
                .processFirstDelivery(request, "{}");

        assertEquals(PaymentSessionStatus.EXPIRED, payment.getStatus());
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        assertEquals(PaymentStatus.UNPAID, cancelled.getPaymentStatus());
        verify(orderRepository, never()).findAllByPaymentIdForUpdateOrdered(any());
    }

    @Test
    void alreadyPaidPaymentDoesNotRepeatOrderSideEffects() {
        Payment payment = payment(PaymentSessionStatus.PAID);
        SePayWebhookRequest request = request();
        when(paymentRepository.findByPaymentCodeForUpdate(request.getCode()))
                .thenReturn(Optional.of(payment));
        when(transactionRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        new SePayWebhookProcessor(paymentRepository, transactionRepository, orderRepository)
                .processFirstDelivery(request, "{}");

        assertEquals(PaymentSessionStatus.PAID, payment.getStatus());
        verify(orderRepository, never()).saveAll(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void partialAndOverpaymentDoNotMarkPaymentPaid() {
        for (BigDecimal amount : List.of(BigDecimal.valueOf(499_999), BigDecimal.valueOf(500_001))) {
            reset(paymentRepository, transactionRepository, orderRepository);
            Payment payment = payment(PaymentSessionStatus.PENDING);
            SePayWebhookRequest request = request();
            request.setTransferAmount(amount);
            when(paymentRepository.findByPaymentCodeForUpdate(request.getCode())).thenReturn(Optional.of(payment));
            when(transactionRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

            SePayWebhookProcessor subject = new SePayWebhookProcessor(paymentRepository, transactionRepository, orderRepository);
            assertThrows(BadRequestException.class, () -> subject.processFirstDelivery(request, "{}"));
            assertEquals(PaymentSessionStatus.PENDING, payment.getStatus());
            verify(orderRepository, never()).saveAll(any());
        }
    }

    @Test
    void accountMismatchDoesNotMarkPaymentPaid() {
        Payment payment = payment(PaymentSessionStatus.PENDING);
        SePayWebhookRequest request = request();
        request.setAccountNumber("WRONG");
        when(paymentRepository.findByPaymentCodeForUpdate(request.getCode())).thenReturn(Optional.of(payment));
        when(transactionRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SePayWebhookProcessor subject = new SePayWebhookProcessor(paymentRepository, transactionRepository, orderRepository);
        assertThrows(BadRequestException.class, () -> subject.processFirstDelivery(request, "{}"));
        assertEquals(PaymentSessionStatus.PENDING, payment.getStatus());
        verify(orderRepository, never()).saveAll(any());
    }

    @Test
    void outgoingTransferIsAuditedWithoutMatchingPayment() {
        SePayWebhookRequest request = request();
        request.setTransferType("out");
        when(transactionRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        new SePayWebhookProcessor(paymentRepository, transactionRepository, orderRepository)
                .processFirstDelivery(request, "{}");

        verify(paymentRepository, never()).findByPaymentCodeForUpdate(any());
        verify(transactionRepository).saveAndFlush(any());
        verify(orderRepository, never()).saveAll(any());
    }

    private Payment payment(PaymentSessionStatus status) {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setPaymentCode("MS123");
        payment.setAccountNumber("123456");
        payment.setAmount(BigDecimal.valueOf(500_000));
        payment.setStatus(status);
        return payment;
    }

    private SePayWebhookRequest request() {
        SePayWebhookRequest request = new SePayWebhookRequest();
        request.setId(99L);
        request.setGateway("Bank");
        request.setReferenceCode("REF99");
        request.setTransferType("in");
        request.setCode("MS123");
        request.setAccountNumber("123456");
        request.setTransferAmount(BigDecimal.valueOf(500_000));
        return request;
    }

    private SePayTransaction existingTransaction(SePayWebhookRequest request, String rawPayload) {
        SePayTransaction transaction = new SePayTransaction();
        transaction.setSePayTransactionId(request.getId());
        transaction.setGateway(request.getGateway());
        transaction.setReferenceCode(request.getReferenceCode());
        transaction.setAccountNumber(request.getAccountNumber());
        transaction.setTransferAmount(request.getTransferAmount());
        transaction.setRawPayload(rawPayload);
        return transaction;
    }

    private Order unpaidOrder() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        return order;
    }
}
