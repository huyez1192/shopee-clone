package com.ecommerce.minishopee.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("Không tìm thấy người dùng với id: " + userId);
    }
}