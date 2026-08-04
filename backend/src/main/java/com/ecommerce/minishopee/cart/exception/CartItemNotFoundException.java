package com.ecommerce.minishopee.cart.exception;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(Long cartItemId) {
        super("Không tìm thấy CartItem với id: " + cartItemId);
    }
}