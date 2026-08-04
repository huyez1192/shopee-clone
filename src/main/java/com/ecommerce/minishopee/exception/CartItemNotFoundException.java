package com.ecommerce.minishopee.exception;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(Long cartItemId) {
        super("Không tìm thấy CartItem với id: " + cartItemId);
    }
}