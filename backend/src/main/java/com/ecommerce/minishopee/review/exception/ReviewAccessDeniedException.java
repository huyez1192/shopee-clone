package com.ecommerce.minishopee.review.exception;

public class ReviewAccessDeniedException extends RuntimeException {
    public ReviewAccessDeniedException(String message) {
        super(message);
    }
}
