package com.ecommerce.minishopee.notification.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationRealtimeListener {
    public static final String NOTIFICATIONS_DESTINATION = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationRealtimeListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        messagingTemplate.convertAndSendToUser(
                event.recipientPrincipal(),
                NOTIFICATIONS_DESTINATION,
                NotificationRealtimeEventResponse.created(event.notification())
        );
    }
}
