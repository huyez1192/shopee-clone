package com.ecommerce.minishopee.review.exception;

public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException(Long id) {
        super("Không tìm thấy đánh giá với id: " + id);
    }
}
