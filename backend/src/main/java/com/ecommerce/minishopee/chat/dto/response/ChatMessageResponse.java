package com.ecommerce.minishopee.chat.dto.response;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        String senderName,
        String content,
        LocalDateTime createdAt,
        LocalDateTime readAt,
        boolean mine
) {
}
