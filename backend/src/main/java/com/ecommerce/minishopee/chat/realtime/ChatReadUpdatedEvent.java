package com.ecommerce.minishopee.chat.realtime;

import java.time.LocalDateTime;

public record ChatReadUpdatedEvent(
        Long conversationId,
        ChatParticipantRoute customer,
        ChatParticipantRoute seller,
        Long readByUserId,
        LocalDateTime readAt
) {
}
