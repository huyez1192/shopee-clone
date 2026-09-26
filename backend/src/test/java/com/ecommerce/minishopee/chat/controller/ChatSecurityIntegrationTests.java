package com.ecommerce.minishopee.chat.controller;

import com.ecommerce.minishopee.chat.exception.ChatConversationNotFoundException;
import com.ecommerce.minishopee.chat.dto.response.ChatConversationResponse;
import com.ecommerce.minishopee.chat.service.ChatService;
import com.ecommerce.minishopee.common.dto.PageResponse;
import com.ecommerce.minishopee.security.jwt.JwtService;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.user.enums.Role;
import com.ecommerce.minishopee.user.enums.UserStatus;
import com.ecommerce.minishopee.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChatSecurityIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired AppUserRepository userRepository;
    @Autowired JwtService jwtService;
    @MockitoBean ChatService chatService;

    private String userToken;
    private String sellerToken;

    @BeforeEach
    void setUp() {
        userToken = token(Role.USER);
        sellerToken = token(Role.SELLER);
        PageResponse<ChatConversationResponse> empty = PageResponse.<ChatConversationResponse>builder()
                .content(List.of()).pageNumber(0).pageSize(20)
                .totalElements(0).totalPages(0).last(true).build();
        when(chatService.getCustomerConversations(any(), anyInt(), anyInt())).thenReturn(empty);
        when(chatService.getSellerConversations(any(), anyInt(), anyInt())).thenReturn(empty);
    }

    @Test
    void anonymousCannotAccessChatEndpoints() throws Exception {
        mockMvc.perform(get("/api/chat/conversations")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/seller/chat/conversations")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/chat/conversations/1/messages").contentType("application/json")
                .content("{\"content\":\"hello\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedCustomerCanUseCustomerInbox() throws Exception {
        mockMvc.perform(get("/api/chat/conversations").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    void sellerCanUseSellerInboxButUserCannot() throws Exception {
        mockMvc.perform(get("/api/seller/chat/conversations").header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/seller/chat/conversations").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedNonParticipantGetsNotFoundAcrossConversationOperations() throws Exception {
        when(chatService.getConversation(any(), eq(999L))).thenThrow(new ChatConversationNotFoundException(999L));
        when(chatService.getMessages(any(), eq(999L), anyInt(), anyInt()))
                .thenThrow(new ChatConversationNotFoundException(999L));
        when(chatService.sendMessage(any(), eq(999L), any()))
                .thenThrow(new ChatConversationNotFoundException(999L));
        mockMvc.perform(get("/api/chat/conversations/999").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/chat/conversations/999/messages").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/chat/conversations/999/messages").header("Authorization", "Bearer " + userToken)
                .contentType("application/json").content("{\"content\":\"hello\"}"))
                .andExpect(status().isNotFound());
    }

    private String token(Role role) {
        String value = role.name().toLowerCase() + "-" + UUID.randomUUID();
        AppUser user = new AppUser(); user.setFullName(value); user.setEmail(value + "@test.local");
        user.setPassword("password"); user.setPhone(value); user.setRole(role); user.setStatus(UserStatus.ACTIVE);
        return jwtService.generateToken(userRepository.saveAndFlush(user));
    }
}
