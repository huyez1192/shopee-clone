package com.ecommerce.minishopee.chat.dto.response;

import java.time.LocalDateTime;

public record ChatConversationResponse(
        Long conversationId,
        ChatParticipantResponse customer,
        ChatShopResponse shop,
        LocalDateTime createdAt,
        LocalDateTime lastMessageAt,
        String lastMessageContent,
        long unreadCount
) {
}
