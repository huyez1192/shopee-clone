package com.ecommerce.minishopee.chat.service;

import com.ecommerce.minishopee.chat.dto.request.SendChatMessageRequest;
import com.ecommerce.minishopee.chat.entity.ChatConversation;
import com.ecommerce.minishopee.chat.entity.ChatMessage;
import com.ecommerce.minishopee.chat.exception.ChatConversationNotFoundException;
import com.ecommerce.minishopee.chat.repository.ChatConversationRepository;
import com.ecommerce.minishopee.chat.repository.ChatMessageRepository;
import com.ecommerce.minishopee.chat.realtime.ChatMessageCreatedEvent;
import com.ecommerce.minishopee.chat.realtime.ChatReadUpdatedEvent;
import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.product.dto.response.ProductResponse;
import com.ecommerce.minishopee.product.service.ProductService;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.shop.enums.ShopStatus;
import com.ecommerce.minishopee.shop.repository.ShopRepository;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.user.enums.Role;
import com.ecommerce.minishopee.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTests {
    @Mock ChatConversationRepository conversationRepository;
    @Mock ChatMessageRepository messageRepository;
    @Mock ProductService productService;
    @Mock ShopRepository shopRepository;
    @Mock ChatConversationCreator conversationCreator;
    @Mock ApplicationEventPublisher eventPublisher;
    private ChatService service;
    private AppUser customer;
    private AppUser seller;
    private AppUser outsider;
    private Shop shop;
    private ChatConversation conversation;

    @BeforeEach
    void setUp() {
        service = new ChatService(conversationRepository, messageRepository, productService, shopRepository,
                conversationCreator, eventPublisher);
        customer = user(1L, Role.USER); seller = user(2L, Role.SELLER); outsider = user(3L, Role.ADMIN);
        shop = new Shop(); shop.setId(10L); shop.setOwner(seller); shop.setName("Shop"); shop.setStatus(ShopStatus.APPROVED);
        conversation = new ChatConversation(); conversation.setId(20L); conversation.setCustomer(customer);
        conversation.setShop(shop); conversation.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void startIsIdempotentAndUsesPublicProductShop() {
        when(productService.getProductById(100L)).thenReturn(ProductResponse.builder().id(100L).shopId(10L).build());
        when(shopRepository.findById(10L)).thenReturn(Optional.of(shop));
        when(conversationRepository.findByCustomer_IdAndShop_Id(1L, 10L)).thenReturn(Optional.of(conversation));

        assertEquals(20L, service.startConversation(customer, 100L).conversationId());
        verifyNoInteractions(conversationCreator);
    }

    @Test
    void uniqueRaceRequeriesWinnerOutsideFailedCreateTransaction() {
        when(productService.getProductById(100L)).thenReturn(ProductResponse.builder().shopId(10L).build());
        when(shopRepository.findById(10L)).thenReturn(Optional.of(shop));
        when(conversationRepository.findByCustomer_IdAndShop_Id(1L, 10L))
                .thenReturn(Optional.empty()).thenReturn(Optional.of(conversation));
        when(conversationCreator.create(customer, shop)).thenThrow(new DataIntegrityViolationException("race"));

        assertEquals(20L, service.startConversation(customer, 100L).conversationId());
        verify(conversationRepository, times(2)).findByCustomer_IdAndShop_Id(1L, 10L);
    }

    @Test
    void selfChatIsRejected() {
        when(productService.getProductById(100L)).thenReturn(ProductResponse.builder().shopId(10L).build());
        when(shopRepository.findById(10L)).thenReturn(Optional.of(shop));
        assertThrows(BadRequestException.class, () -> service.startConversation(seller, 100L));
    }

    @Test
    void userSellerAndAdminCanAllActAsCustomerOfAnotherShop() {
        when(productService.getProductById(100L)).thenReturn(ProductResponse.builder().shopId(10L).build());
        when(shopRepository.findById(10L)).thenReturn(Optional.of(shop));
        List<AppUser> customers = List.of(customer, user(4L, Role.SELLER), outsider);
        for (AppUser actor : customers) {
            ChatConversation actorConversation = new ChatConversation();
            actorConversation.setId(100L + actor.getId()); actorConversation.setCustomer(actor);
            actorConversation.setShop(shop); actorConversation.setCreatedAt(LocalDateTime.now());
            when(conversationRepository.findByCustomer_IdAndShop_Id(actor.getId(), 10L))
                    .thenReturn(Optional.of(actorConversation));
            assertEquals(actorConversation.getId(), service.startConversation(actor, 100L).conversationId());
        }
    }

    @Test
    void sendTrimsContentPersistsMessageAndUpdatesLastActivityAtomically() {
        when(conversationRepository.findDetailedById(20L)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any())).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0); message.setId(30L); return message;
        });

        var response = service.sendMessage(customer, 20L, new SendChatMessageRequest("  hello  "));
        ArgumentCaptor<ChatMessage> message = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepository).save(message.capture());
        assertEquals("hello", message.getValue().getContent());
        assertEquals("hello", response.content());
        assertTrue(response.mine());
        assertNotNull(conversation.getLastMessageAt());
        verify(conversationRepository).save(conversation);
        ArgumentCaptor<ChatMessageCreatedEvent> event = ArgumentCaptor.forClass(ChatMessageCreatedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals("seller2@test.local", event.getValue().seller().principalName());
        assertEquals("user1@test.local", event.getValue().customer().principalName());
    }

    @Test
    void blankTooLongAndUnavailableShopAreRejected() {
        when(conversationRepository.findDetailedById(20L)).thenReturn(Optional.of(conversation));
        assertThrows(BadRequestException.class, () -> service.sendMessage(customer, 20L, new SendChatMessageRequest("   ")));
        assertThrows(BadRequestException.class, () -> service.sendMessage(customer, 20L, new SendChatMessageRequest("x".repeat(2001))));
        shop.setStatus(ShopStatus.LOCKED);
        assertThrows(BadRequestException.class, () -> service.sendMessage(customer, 20L, new SendChatMessageRequest("hello")));
        when(messageRepository.findByConversation_Id(eq(20L), any())).thenReturn(new PageImpl<>(List.of()));
        assertDoesNotThrow(() -> service.getMessages(customer, 20L, 0, 30));
        verify(messageRepository, never()).save(any());
    }

    @Test
    void nonParticipantCannotReadSendOrMarkConversation() {
        when(conversationRepository.findDetailedById(20L)).thenReturn(Optional.of(conversation));
        assertThrows(ChatConversationNotFoundException.class, () -> service.getConversation(outsider, 20L));
        assertThrows(ChatConversationNotFoundException.class, () -> service.getMessages(outsider, 20L, 0, 30));
        assertThrows(ChatConversationNotFoundException.class,
                () -> service.sendMessage(outsider, 20L, new SendChatMessageRequest("hello")));
        assertThrows(ChatConversationNotFoundException.class, () -> service.markRead(outsider, 20L));
    }

    @Test
    void messageHistoryMapsMineAndMarkReadOnlyDelegatesCounterpartBulkUpdate() {
        when(conversationRepository.findDetailedById(20L)).thenReturn(Optional.of(conversation));
        ChatMessage own = message(31L, customer, "own", LocalDateTime.of(2040, 1, 1, 11, 0));
        ChatMessage reply = message(30L, seller, "reply", LocalDateTime.of(2040, 1, 1, 10, 0));
        when(messageRepository.findByConversation_Id(eq(20L), any()))
                .thenReturn(new PageImpl<>(List.of(own, reply)));

        var history = service.getMessages(customer, 20L, 0, 30);
        assertTrue(history.getContent().get(0).mine());
        assertFalse(history.getContent().get(1).mine());
        service.markRead(customer, 20L);
        verify(messageRepository).markCounterpartMessagesRead(eq(20L), eq(1L), any(LocalDateTime.class));
        verify(eventPublisher).publishEvent(any(ChatReadUpdatedEvent.class));
    }

    @Test
    void customerAndSellerUnreadUseSeparateInboxSemantics() {
        when(messageRepository.countCustomerUnread(1L)).thenReturn(4L);
        when(shopRepository.findByOwnerId(2L)).thenReturn(Optional.of(shop));
        when(messageRepository.countSellerUnread(10L, 2L)).thenReturn(3L);
        assertEquals(4L, service.getCustomerUnreadCount(customer).count());
        assertEquals(3L, service.getSellerUnreadCount(seller).count());
    }

    private ChatMessage message(Long id, AppUser sender, String content, LocalDateTime createdAt) {
        ChatMessage message = new ChatMessage(); message.setId(id); message.setConversation(conversation);
        message.setSender(sender); message.setContent(content); message.setCreatedAt(createdAt); return message;
    }

    private AppUser user(Long id, Role role) {
        AppUser user = new AppUser(); user.setId(id); user.setFullName(role.name()); user.setRole(role);
        user.setStatus(UserStatus.ACTIVE); user.setEmail(role.name().toLowerCase() + id + "@test.local"); return user;
    }
}
