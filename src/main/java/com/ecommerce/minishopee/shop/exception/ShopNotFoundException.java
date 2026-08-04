package com.ecommerce.minishopee.shop.exception;

public class ShopNotFoundException extends RuntimeException {
    public ShopNotFoundException(Long id) {
        super("Không tìm thấy shop với id: " + id);
    }

    public ShopNotFoundException(String message) {
        super(message);
    }
}







