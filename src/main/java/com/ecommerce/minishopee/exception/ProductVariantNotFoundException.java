package com.ecommerce.minishopee.exception;

public class ProductVariantNotFoundException extends RuntimeException {

    public ProductVariantNotFoundException(Long id) {
        super("Không tìm thấy SKU với id: " + id);
    }
}