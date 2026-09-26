package com.ecommerce.minishopee.notification.realtime;

import com.ecommerce.minishopee.notification.dto.response.NotificationResponse;

public record NotificationRealtimeEventResponse(
        NotificationRealtimeEventType type,
        NotificationResponse notification
) {
    public static NotificationRealtimeEventResponse created(NotificationResponse notification) {
        return new NotificationRealtimeEventResponse(NotificationRealtimeEventType.NOTIFICATION_CREATED, notification);
    }
}
