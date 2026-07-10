package com.example.productcrudapi.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Không tìm thấy sản phẩm với id: " + id);
    }
}
