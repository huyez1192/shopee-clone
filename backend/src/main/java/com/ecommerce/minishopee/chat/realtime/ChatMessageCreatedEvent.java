package com.ecommerce.minishopee.chat.realtime;

public record ChatMessageCreatedEvent(
        ChatParticipantRoute customer,
        ChatParticipantRoute seller,
        RealtimeChatMessageResponse message
) {
}
