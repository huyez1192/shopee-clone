package com.ecommerce.minishopee.chat.repository;

import com.ecommerce.minishopee.chat.entity.ChatConversation;
import com.ecommerce.minishopee.chat.entity.ChatMessage;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.shop.enums.ShopStatus;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.user.enums.Role;
import com.ecommerce.minishopee.user.enums.UserStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChatRepositoryIntegrationTests {
    @Autowired EntityManager entityManager;
    @Autowired ChatConversationRepository conversationRepository;
    @Autowired ChatMessageRepository messageRepository;

    private AppUser customer;
    private AppUser seller;
    private AppUser outsider;
    private Shop shop;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        customer = user("customer-" + suffix, Role.USER);
        seller = user("seller-" + suffix, Role.SELLER);
        outsider = user("outsider-" + suffix, Role.ADMIN);
        shop = new Shop();
        shop.setOwner(seller); shop.setName("Chat shop " + suffix); shop.setAddress("Address");
        shop.setContactPhone("0123456789"); shop.setStatus(ShopStatus.APPROVED);
        entityManager.persist(shop);
        entityManager.flush();
    }

    @Test
    void databaseRejectsDuplicateCustomerShopConversation() {
        conversation(customer, shop);
        entityManager.flush();
        assertThrows(jakarta.persistence.PersistenceException.class, () -> conversation(customer, shop));
    }

    @Test
    void conversationPagesAreOwnerScopedAndSortedByLatestActivityThenId() {
        ChatConversation older = conversation(customer, shop);
        older.setLastMessageAt(LocalDateTime.of(2040, 1, 1, 10, 0));
        ChatConversation newer = conversation(customer, shop("Other " + UUID.randomUUID(), seller("second-" + UUID.randomUUID())));
        newer.setLastMessageAt(LocalDateTime.of(2040, 1, 2, 10, 0));
        conversation(outsider, shop);
        entityManager.flush();

        var customerPage = conversationRepository.findCustomerConversations(customer.getId(), PageRequest.of(0, 20));
        var sellerPage = conversationRepository.findShopConversations(shop.getId(), PageRequest.of(0, 20));
        assertEquals(List.of(newer.getId(), older.getId()), customerPage.map(ChatConversation::getId).getContent());
        assertEquals(2, sellerPage.getTotalElements());
    }

    @Test
    void latestMessageUnreadAggregatesAndHistoryOrderingAreCorrectWithoutPerRowQueries() {
        ChatConversation conversation = conversation(customer, shop);
        ChatMessage first = message(conversation, customer, "first", LocalDateTime.of(2040, 1, 1, 9, 0));
        ChatMessage reply = message(conversation, seller, "reply", LocalDateTime.of(2040, 1, 1, 10, 0));
        ChatMessage latest = message(conversation, seller, "latest", LocalDateTime.of(2040, 1, 1, 11, 0));
        entityManager.flush();

        assertEquals(latest.getId(), messageRepository.findLatestForConversations(List.of(conversation.getId())).getFirst().getId());
        assertEquals(2L, messageRepository.countUnreadByConversationIds(List.of(conversation.getId()), customer.getId())
                .getFirst().getUnreadCount());
        assertEquals(1L, messageRepository.countSellerUnread(shop.getId(), seller.getId()));
        var history = messageRepository.findByConversation_Id(conversation.getId(),
                PageRequest.of(0, 2, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.desc("createdAt"),
                        org.springframework.data.domain.Sort.Order.desc("id"))));
        assertEquals(List.of(latest.getId(), reply.getId()), history.map(ChatMessage::getId).getContent());
        assertNotNull(first.getId());
    }

    @Test
    void markReadOnlyTouchesCounterpartMessagesAndIsIdempotent() {
        ChatConversation conversation = conversation(customer, shop);
        ChatMessage own = message(conversation, customer, "own", LocalDateTime.now());
        ChatMessage counterpart = message(conversation, seller, "counterpart", LocalDateTime.now());
        entityManager.flush();

        assertEquals(1, messageRepository.markCounterpartMessagesRead(conversation.getId(), customer.getId(), LocalDateTime.now()));
        assertEquals(0, messageRepository.markCounterpartMessagesRead(conversation.getId(), customer.getId(), LocalDateTime.now()));
        entityManager.clear();
        assertNull(messageRepository.findById(own.getId()).orElseThrow().getReadAt());
        assertNotNull(messageRepository.findById(counterpart.getId()).orElseThrow().getReadAt());
        assertEquals(0L, messageRepository.countCustomerUnread(customer.getId()));
    }

    private AppUser user(String key, Role role) {
        AppUser user = new AppUser(); user.setFullName(key); user.setEmail(key + "@test.local");
        user.setPassword("password"); user.setPhone(key); user.setRole(role); user.setStatus(UserStatus.ACTIVE);
        entityManager.persist(user); return user;
    }

    private AppUser seller(String key) { return user(key, Role.SELLER); }

    private Shop shop(String name, AppUser owner) {
        Shop result = new Shop(); result.setOwner(owner); result.setName(name); result.setAddress("Address");
        result.setContactPhone("0123456789"); result.setStatus(ShopStatus.APPROVED); entityManager.persist(result); return result;
    }

    private ChatConversation conversation(AppUser buyer, Shop target) {
        ChatConversation result = new ChatConversation(); result.setCustomer(buyer); result.setShop(target);
        entityManager.persist(result); return result;
    }

    private ChatMessage message(ChatConversation conversation, AppUser sender, String content, LocalDateTime time) {
        ChatMessage result = new ChatMessage(); result.setConversation(conversation); result.setSender(sender);
        result.setContent(content); result.setCreatedAt(time); entityManager.persist(result); return result;
    }
}
