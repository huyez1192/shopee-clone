package com.ecommerce.minishopee.chat.realtime;

public record ChatParticipantRoute(
        Long userId,
        String principalName,
        ChatRecipientContext context
) {
}
