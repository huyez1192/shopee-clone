package com.ecommerce.minishopee.chat.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ChatRealtimeListener {
    public static final String CHAT_EVENTS_DESTINATION = "/queue/chat/events";

    private final SimpMessagingTemplate messagingTemplate;

    public ChatRealtimeListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageCreated(ChatMessageCreatedEvent event) {
        send(event.customer(), ChatRealtimeEventResponse.messageCreated(
                event.customer().context(), event.message()));
        send(event.seller(), ChatRealtimeEventResponse.messageCreated(
                event.seller().context(), event.message()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReadUpdated(ChatReadUpdatedEvent event) {
        send(event.customer(), ChatRealtimeEventResponse.readUpdated(
                event.conversationId(), event.customer().context(), event.readByUserId(), event.readAt()));
        send(event.seller(), ChatRealtimeEventResponse.readUpdated(
                event.conversationId(), event.seller().context(), event.readByUserId(), event.readAt()));
    }

    private void send(ChatParticipantRoute route, ChatRealtimeEventResponse payload) {
        messagingTemplate.convertAndSendToUser(route.principalName(), CHAT_EVENTS_DESTINATION, payload);
    }
}
