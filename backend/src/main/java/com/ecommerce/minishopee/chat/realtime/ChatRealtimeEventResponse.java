package com.ecommerce.minishopee.chat.realtime;

import java.time.LocalDateTime;

public record ChatRealtimeEventResponse(
        ChatRealtimeEventType type,
        Long conversationId,
        ChatRecipientContext recipientContext,
        RealtimeChatMessageResponse message,
        String lastMessageContent,
        LocalDateTime lastMessageAt,
        Long messageId,
        Long readByUserId,
        LocalDateTime readAt
) {
    public static ChatRealtimeEventResponse messageCreated(
            ChatRecipientContext context,
            RealtimeChatMessageResponse message
    ) {
        return new ChatRealtimeEventResponse(
                ChatRealtimeEventType.CHAT_MESSAGE_CREATED,
                message.conversationId(), context, message, message.content(),
                message.createdAt(), message.id(), null, null
        );
    }

    public static ChatRealtimeEventResponse readUpdated(
            Long conversationId,
            ChatRecipientContext context,
            Long readByUserId,
            LocalDateTime readAt
    ) {
        return new ChatRealtimeEventResponse(
                ChatRealtimeEventType.CHAT_READ_UPDATED,
                conversationId, context, null, null, null, null, readByUserId, readAt
        );
    }
}
