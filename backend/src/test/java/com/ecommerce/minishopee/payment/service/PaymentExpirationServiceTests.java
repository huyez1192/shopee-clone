package com.ecommerce.minishopee.payment.service;

import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.order.entity.OrderItem;
import com.ecommerce.minishopee.order.enums.OrderStatus;
import com.ecommerce.minishopee.order.repository.OrderRepository;
import com.ecommerce.minishopee.payment.entity.Payment;
import com.ecommerce.minishopee.payment.enums.PaymentSessionStatus;
import com.ecommerce.minishopee.payment.repository.PaymentRepository;
import com.ecommerce.minishopee.product.entity.ProductVariant;
import com.ecommerce.minishopee.product.enums.ProductVariantStatus;
import com.ecommerce.minishopee.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentExpirationServiceTests {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ProductVariantRepository variantRepository;

    private PaymentExpirationService service;

    @BeforeEach
    void setUp() {
        service = new PaymentExpirationService(paymentRepository, orderRepository, variantRepository);
    }

    @Test
    void expiredPendingPaymentCancelsOrdersAndRestoresLockedStockOnce() {
        Payment payment = expiredPendingPayment();
        ProductVariant variant = variant(11L, 8);
        Order order = pendingOrder(item(variant, 2));
        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(orderRepository.findAllByPaymentIdForUpdateOrdered(payment.getId()))
                .thenReturn(List.of(order));
        when(variantRepository.findAllByIdsForUpdateOrdered(List.of(variant.getId())))
                .thenReturn(List.of(variant));

        service.expirePayment(payment.getId());
        service.expirePayment(payment.getId());

        assertEquals(PaymentSessionStatus.EXPIRED, payment.getStatus());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(10, variant.getStockQuantity());
        assertNotNull(order.getCancelledAt());
        verify(variantRepository, times(1)).findAllByIdsForUpdateOrdered(any());
    }

    @Test
    void paidPaymentIsIgnored() {
        Payment payment = expiredPendingPayment();
        payment.setStatus(PaymentSessionStatus.PAID);
        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));

        service.expirePayment(payment.getId());

        verifyNoInteractions(orderRepository, variantRepository);
        assertEquals(PaymentSessionStatus.PAID, payment.getStatus());
    }

    @Test
    void paymentBeforeExpiryIsIgnored() {
        Payment payment = expiredPendingPayment();
        payment.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));

        service.expirePayment(payment.getId());

        verifyNoInteractions(orderRepository, variantRepository);
        assertEquals(PaymentSessionStatus.PENDING, payment.getStatus());
    }

    @Test
    void multipleOrdersAggregateQuantitiesAndLockVariantsInAscendingOrder() {
        Payment payment = expiredPendingPayment();
        ProductVariant first = variant(10L, 5);
        ProductVariant second = variant(20L, 7);
        Order orderA = pendingOrder(item(second, 2), item(first, 1));
        Order orderB = pendingOrder(item(first, 3));
        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(orderRepository.findAllByPaymentIdForUpdateOrdered(payment.getId()))
                .thenReturn(List.of(orderA, orderB));
        when(variantRepository.findAllByIdsForUpdateOrdered(List.of(10L, 20L)))
                .thenReturn(List.of(first, second));

        service.expirePayment(payment.getId());

        assertEquals(9, first.getStockQuantity());
        assertEquals(9, second.getStockQuantity());
        assertEquals(OrderStatus.CANCELLED, orderA.getStatus());
        assertEquals(OrderStatus.CANCELLED, orderB.getStatus());
        verify(variantRepository).findAllByIdsForUpdateOrdered(List.of(10L, 20L));
    }

    private Payment expiredPendingPayment() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setStatus(PaymentSessionStatus.PENDING);
        payment.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        return payment;
    }

    private ProductVariant variant(Long id, int stock) {
        ProductVariant variant = new ProductVariant();
        variant.setId(id);
        variant.setStockQuantity(stock);
        variant.setStatus(ProductVariantStatus.ACTIVE);
        return variant;
    }

    private Order pendingOrder(OrderItem... items) {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);
        for (OrderItem item : items) {
            order.addItem(item);
        }
        return order;
    }

    private OrderItem item(ProductVariant variant, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductVariant(variant);
        item.setQuantity(quantity);
        return item;
    }
}
