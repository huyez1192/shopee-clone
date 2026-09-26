package com.ecommerce.minishopee.chat.realtime;

import java.time.LocalDateTime;

public record RealtimeChatMessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        String senderName,
        String content,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}
