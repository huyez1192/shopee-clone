package com.ecommerce.minishopee.order.service;

import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.order.enums.PaymentMethod;
import com.ecommerce.minishopee.order.enums.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class OrderPaymentPolicy {

    public void validateCanFulfill(Order order) {
        if (order.getPaymentMethod() == PaymentMethod.SEPAY
                && order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BadRequestException(
                    "Đơn hàng SePay không thể được xử lý trước khi thanh toán hoàn tất."
            );
        }
    }

    public void validateCanCancelIndividually(Order order) {
        if (order.getPaymentMethod() != PaymentMethod.SEPAY) {
            return;
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException(
                    "Đơn hàng online đã thanh toán cần quy trình hoàn tiền trước khi hủy."
            );
        }

        throw new BadRequestException(
                "Không thể hủy riêng một đơn hàng SePay đang chờ thanh toán; phiên thanh toán được quản lý theo toàn bộ nhóm đơn hàng."
        );
    }
}
