package com.ecommerce.minishopee.notification.exception;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(Long id) {
        super("Không tìm thấy thông báo: " + id);
    }
}
