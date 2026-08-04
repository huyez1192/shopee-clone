package com.ecommerce.minishopee.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long orderId) {
        super("Không tìm thấy đơn hàng với id: " + orderId);
    }
}