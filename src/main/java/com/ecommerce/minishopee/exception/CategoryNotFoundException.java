package com.ecommerce.minishopee.exception;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Long id) {
        super("Không tìm thấy danh mục với id:" + id);
    }
}
