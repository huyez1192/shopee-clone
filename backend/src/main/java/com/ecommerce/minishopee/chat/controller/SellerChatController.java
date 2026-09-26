package com.ecommerce.minishopee.chat.controller;

import com.ecommerce.minishopee.chat.dto.response.ChatConversationResponse;
import com.ecommerce.minishopee.chat.dto.response.ChatUnreadCountResponse;
import com.ecommerce.minishopee.chat.service.ChatService;
import com.ecommerce.minishopee.common.dto.PageResponse;
import com.ecommerce.minishopee.user.entity.AppUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seller/chat")
public class SellerChatController {
    private final ChatService chatService;

    public SellerChatController(ChatService chatService) { this.chatService = chatService; }

    @GetMapping("/conversations")
    public PageResponse<ChatConversationResponse> conversations(@AuthenticationPrincipal AppUser seller,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return chatService.getSellerConversations(seller, page, size);
    }

    @GetMapping("/unread-count")
    public ChatUnreadCountResponse unread(@AuthenticationPrincipal AppUser seller) {
        return chatService.getSellerUnreadCount(seller);
    }
}
