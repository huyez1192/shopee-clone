package com.ecommerce.minishopee.chat.controller;

import com.ecommerce.minishopee.chat.dto.request.SendChatMessageRequest;
import com.ecommerce.minishopee.chat.dto.response.*;
import com.ecommerce.minishopee.chat.service.ChatService;
import com.ecommerce.minishopee.common.dto.PageResponse;
import com.ecommerce.minishopee.user.entity.AppUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) { this.chatService = chatService; }

    @PostMapping("/conversations/products/{productId}")
    public ChatConversationResponse start(@AuthenticationPrincipal AppUser user, @PathVariable Long productId) {
        return chatService.startConversation(user, productId);
    }

    @GetMapping("/conversations")
    public PageResponse<ChatConversationResponse> conversations(@AuthenticationPrincipal AppUser user,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return chatService.getCustomerConversations(user, page, size);
    }

    @GetMapping("/conversations/{id}")
    public ChatConversationResponse conversation(@AuthenticationPrincipal AppUser user, @PathVariable Long id) {
        return chatService.getConversation(user, id);
    }

    @GetMapping("/conversations/{id}/messages")
    public PageResponse<ChatMessageResponse> messages(@AuthenticationPrincipal AppUser user, @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "30") int size) {
        return chatService.getMessages(user, id, page, size);
    }

    @PostMapping("/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse send(@AuthenticationPrincipal AppUser user, @PathVariable Long id,
            @Valid @RequestBody SendChatMessageRequest request) {
        return chatService.sendMessage(user, id, request);
    }

    @PatchMapping("/conversations/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal AppUser user, @PathVariable Long id) {
        chatService.markRead(user, id);
    }

    @GetMapping("/unread-count")
    public ChatUnreadCountResponse unread(@AuthenticationPrincipal AppUser user) {
        return chatService.getCustomerUnreadCount(user);
    }
}
