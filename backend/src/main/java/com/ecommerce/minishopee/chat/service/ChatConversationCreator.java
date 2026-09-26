package com.ecommerce.minishopee.chat.service;

import com.ecommerce.minishopee.chat.entity.ChatConversation;
import com.ecommerce.minishopee.chat.repository.ChatConversationRepository;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.user.entity.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatConversationCreator {
    private final ChatConversationRepository conversationRepository;

    public ChatConversationCreator(ChatConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long create(AppUser customer, Shop shop) {
        ChatConversation conversation = new ChatConversation();
        conversation.setCustomer(customer);
        conversation.setShop(shop);
        return conversationRepository.saveAndFlush(conversation).getId();
    }
}
