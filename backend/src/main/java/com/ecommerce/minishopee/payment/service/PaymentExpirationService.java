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
import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.notification.service.NotificationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PaymentExpirationService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final NotificationService notificationService;

    public PaymentExpirationService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            ProductVariantRepository productVariantRepository,
            NotificationService notificationService
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.productVariantRepository = productVariantRepository;
        this.notificationService = notificationService;
    }

    public List<Long> findExpiredPaymentIds() {
        return paymentRepository.findExpiredPaymentIds(
                                    PaymentSessionStatus.PENDING,
                                    LocalDateTime.now()
                            );
    }

    @Transactional
    public void expirePayment(Long paymentId) {
        Payment payment = paymentRepository
                                .findByIdForUpdate(paymentId)
                                .orElse(null);

        if (payment == null) {
            return;
        }

        /*
         * Có thể webhook vừa thanh toán xong
         * trước khi scheduler lock được Payment.
         */
        if (payment.getStatus() != PaymentSessionStatus.PENDING) {
            return;
        }

        /*
         * Kiểm tra lại thời gian sau khi lock.
         */
        LocalDateTime expiresAt = payment.getExpiresAt();

        if (expiresAt == null || expiresAt.isAfter(LocalDateTime.now())) {
            return;
        }

        List<Order> lockedOrders = orderRepository
                .findAllByPaymentIdForUpdateOrdered(payment.getId());

        Map<Long, Integer> quantityByVariantId = new LinkedHashMap<>();

        for (Order order : lockedOrders) {

            /*
             * SEPAY chưa thanh toán thì seller
             * không nên được xác nhận Order.
             *
             * Hiện ta chỉ tự động cancel
             * Order vẫn đang PENDING.
             */
            if (order.getStatus() != OrderStatus.PENDING) {
                continue;
            }

            for (OrderItem item : order.getItems()) {
                if (item.getProductVariant() != null) {
                    quantityByVariantId.merge(
                            item.getProductVariant().getId(),
                            item.getQuantity(),
                            Integer::sum
                    );
                }
            }
        }

        restoreStock(quantityByVariantId);

        LocalDateTime cancelledAt = LocalDateTime.now();
        List<Order> cancelledOrders = new ArrayList<>();

        for (Order order : lockedOrders) {
            if (order.getStatus() != OrderStatus.PENDING) {
                continue;
            }

            order.setStatus(OrderStatus.CANCELLED);

            order.setCancelledAt(cancelledAt);

            order.setCancelReason(
                    "Phiên thanh toán SePay đã hết hạn."
            );
            cancelledOrders.add(order);
        }

        payment.setStatus(PaymentSessionStatus.EXPIRED);

        paymentRepository.save(payment);
        orderRepository.saveAll(lockedOrders);

        cancelledOrders.forEach(order -> notificationService.notifyOrderCancelledToBuyer(order, true));
    }

    private void restoreStock(Map<Long, Integer> quantityByVariantId) {
        if (quantityByVariantId.isEmpty()) {
            return;
        }

        List<Long> sortedVariantIds = quantityByVariantId.keySet()
                .stream()
                .sorted()
                .toList();

        List<ProductVariant> lockedVariants = productVariantRepository
                .findAllByIdsForUpdateOrdered(sortedVariantIds);

        if (lockedVariants.size() != sortedVariantIds.size()) {
            throw new BadRequestException("Không tìm thấy đầy đủ SKU cần hoàn tồn kho.");
        }

        for (ProductVariant variant : lockedVariants) {

            int currentStock =
                    variant.getStockQuantity() == null
                            ? 0
                            : variant.getStockQuantity();

            int restoredStock =
                    currentStock
                            + quantityByVariantId.get(variant.getId());

            variant.setStockQuantity(
                    restoredStock
            );

            /*
             * Nếu SKU bị OUT_OF_STOCK chỉ vì
             * đơn vừa mua hết stock thì bật lại.
             *
             * Không tự bật SKU đang DELETED,
             * INACTIVE... nếu hệ thống có các
             * trạng thái đó.
             */
            if (
                    variant.getStatus()
                            == ProductVariantStatus.OUT_OF_STOCK
                            && restoredStock > 0
            ) {
                variant.setStatus(
                        ProductVariantStatus.ACTIVE
                );
            }
        }

        productVariantRepository.saveAll(lockedVariants);
    }
}
