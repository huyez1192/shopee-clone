package com.ecommerce.minishopee.chat.repository;

public interface ConversationUnreadCountProjection {
    Long getConversationId();
    Long getUnreadCount();
}
