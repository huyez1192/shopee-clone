package com.ecommerce.minishopee.chat.exception;

public class ChatConversationNotFoundException extends RuntimeException {
    public ChatConversationNotFoundException(Long conversationId) {
        super("Không tìm thấy cuộc trò chuyện với id: " + conversationId);
    }
}
