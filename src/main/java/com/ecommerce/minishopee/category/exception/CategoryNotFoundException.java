package com.ecommerce.minishopee.category.exception;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Long id) {
        super("Không tìm thấy danh mục với id:" + id);
    }
}
