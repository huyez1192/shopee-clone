package com.ecommerce.minishopee.payment.service;

import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.order.entity.OrderItem;
import com.ecommerce.minishopee.order.enums.OrderStatus;
import com.ecommerce.minishopee.payment.entity.Payment;
import com.ecommerce.minishopee.payment.enums.PaymentSessionStatus;
import com.ecommerce.minishopee.payment.repository.PaymentRepository;
import com.ecommerce.minishopee.product.entity.ProductVariant;
import com.ecommerce.minishopee.product.enums.ProductVariantStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentExpirationService {

    private final PaymentRepository paymentRepository;

    public PaymentExpirationService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
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

        /*
         * Đánh dấu Payment hết hạn.
         */
        payment.setStatus(PaymentSessionStatus.EXPIRED);

        /*
         * Hủy từng Order và hoàn lại stock.
         */
        for (Order order : payment.getOrders()) {

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

            restoreStock(order);

            order.setStatus(OrderStatus.CANCELLED);

            order.setCancelledAt(LocalDateTime.now());

            order.setCancelReason(
                    "Phiên thanh toán SePay đã hết hạn."
            );
        }

        paymentRepository.save(payment);
    }

    private void restoreStock(
            Order order
    ) {
        for (OrderItem item : order.getItems()) {
            ProductVariant variant =
                    item.getProductVariant();

            if (variant == null) {
                continue;
            }

            int currentStock =
                    variant.getStockQuantity() == null
                            ? 0
                            : variant.getStockQuantity();

            int restoredStock =
                    currentStock
                            + item.getQuantity();

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
    }
}