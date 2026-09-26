package com.ecommerce.minishopee.chat.realtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class ChatRealtimeListenerIntegrationTests {
    @Autowired ApplicationEventPublisher publisher;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean SimpMessagingTemplate messagingTemplate;
    private ChatParticipantRoute customer;
    private ChatParticipantRoute seller;
    private RealtimeChatMessageResponse message;

    @BeforeEach
    void setUp() {
        reset(messagingTemplate);
        customer = new ChatParticipantRoute(1L, "customer@test.local", ChatRecipientContext.CUSTOMER);
        seller = new ChatParticipantRoute(2L, "seller@test.local", ChatRecipientContext.SELLER);
        message = new RealtimeChatMessageResponse(9L, 5L, 1L, "Customer", "hello",
                LocalDateTime.of(2040, 1, 1, 10, 0), null);
    }

    @Test
    void committedMessageRoutesPersistedPayloadToCustomerAndSeller() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                publisher.publishEvent(new ChatMessageCreatedEvent(customer, seller, message)));
        verify(messagingTemplate).convertAndSendToUser(eq("customer@test.local"),
                eq(ChatRealtimeListener.CHAT_EVENTS_DESTINATION), argThat(this::customerMessage));
        verify(messagingTemplate).convertAndSendToUser(eq("seller@test.local"),
                eq(ChatRealtimeListener.CHAT_EVENTS_DESTINATION), argThat(this::sellerMessage));
    }

    @Test
    void rollbackDoesNotEmitRealtimeEvent() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            publisher.publishEvent(new ChatMessageCreatedEvent(customer, seller, message));
            status.setRollbackOnly();
        });
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void committedReadRoutesConversationLevelEventToBothParticipants() {
        LocalDateTime readAt = LocalDateTime.of(2040, 1, 1, 11, 0);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> publisher.publishEvent(
                new ChatReadUpdatedEvent(5L, customer, seller, 2L, readAt)));
        verify(messagingTemplate, times(2)).convertAndSendToUser(anyString(),
                eq(ChatRealtimeListener.CHAT_EVENTS_DESTINATION), argThat(payload -> {
                    ChatRealtimeEventResponse event = (ChatRealtimeEventResponse) payload;
                    return event.type() == ChatRealtimeEventType.CHAT_READ_UPDATED
                            && event.readByUserId() == 2L && readAt.equals(event.readAt());
                }));
    }

    @Test
    void outboundPayloadDoesNotExposeSensitiveAccountFields() {
        var componentNames = java.util.Arrays.stream(ChatRealtimeEventResponse.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName).toList();
        var messageNames = java.util.Arrays.stream(RealtimeChatMessageResponse.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName).toList();
        assertFalse(componentNames.contains("email") || componentNames.contains("password") || componentNames.contains("phone"));
        assertFalse(messageNames.contains("email") || messageNames.contains("password") || messageNames.contains("phone"));
    }

    private boolean customerMessage(Object payload) {
        ChatRealtimeEventResponse event = (ChatRealtimeEventResponse) payload;
        return event.type() == ChatRealtimeEventType.CHAT_MESSAGE_CREATED
                && event.recipientContext() == ChatRecipientContext.CUSTOMER
                && event.message().id() == 9L;
    }

    private boolean sellerMessage(Object payload) {
        ChatRealtimeEventResponse event = (ChatRealtimeEventResponse) payload;
        return event.type() == ChatRealtimeEventType.CHAT_MESSAGE_CREATED
                && event.recipientContext() == ChatRecipientContext.SELLER
                && event.message().id() == 9L;
    }
}
