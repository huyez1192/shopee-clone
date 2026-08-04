package com.ecommerce.minishopee.user.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("Không tìm thấy người dùng với id: " + userId);
    }
}