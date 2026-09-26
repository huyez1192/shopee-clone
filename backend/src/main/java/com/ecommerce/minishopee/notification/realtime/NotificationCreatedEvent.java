package com.ecommerce.minishopee.notification.realtime;

import com.ecommerce.minishopee.notification.dto.response.NotificationResponse;

public record NotificationCreatedEvent(
        String recipientPrincipal,
        NotificationResponse notification
) {
}
