package com.ecommerce.minishopee.review.exception;

public class OrderItemNotFoundException extends RuntimeException {
    public OrderItemNotFoundException(Long id) {
        super("Không tìm thấy sản phẩm trong đơn hàng với id: " + id);
    }
}
