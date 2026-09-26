package com.ecommerce.minishopee.chat.service;

import com.ecommerce.minishopee.chat.dto.request.SendChatMessageRequest;
import com.ecommerce.minishopee.chat.dto.response.*;
import com.ecommerce.minishopee.chat.entity.ChatConversation;
import com.ecommerce.minishopee.chat.entity.ChatMessage;
import com.ecommerce.minishopee.chat.exception.ChatConversationNotFoundException;
import com.ecommerce.minishopee.chat.repository.ChatConversationRepository;
import com.ecommerce.minishopee.chat.repository.ChatMessageRepository;
import com.ecommerce.minishopee.chat.repository.ConversationUnreadCountProjection;
import com.ecommerce.minishopee.chat.realtime.*;
import com.ecommerce.minishopee.common.dto.PageResponse;
import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.product.dto.response.ProductResponse;
import com.ecommerce.minishopee.product.service.ProductService;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.shop.enums.ShopStatus;
import com.ecommerce.minishopee.shop.exception.ShopNotFoundException;
import com.ecommerce.minishopee.shop.repository.ShopRepository;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.user.enums.UserStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChatService {
    private static final int MAX_PAGE_SIZE = 100;
    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ProductService productService;
    private final ShopRepository shopRepository;
    private final ChatConversationCreator conversationCreator;
    private final ApplicationEventPublisher eventPublisher;

    public ChatService(ChatConversationRepository conversationRepository,
                       ChatMessageRepository messageRepository,
                       ProductService productService,
                       ShopRepository shopRepository,
                       ChatConversationCreator conversationCreator,
                       ApplicationEventPublisher eventPublisher) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.productService = productService;
        this.shopRepository = shopRepository;
        this.conversationCreator = conversationCreator;
        this.eventPublisher = eventPublisher;
    }

    public ChatConversationResponse startConversation(AppUser currentUser, Long productId) {
        ProductResponse product = productService.getProductById(productId);
        Shop shop = shopRepository.findById(product.getShopId())
                .orElseThrow(() -> new ShopNotFoundException(product.getShopId()));
        if (Objects.equals(shop.getOwner().getId(), currentUser.getId())) {
            throw new BadRequestException("Không thể trò chuyện với shop của chính bạn.");
        }
        Optional<ChatConversation> existing = conversationRepository
                .findByCustomer_IdAndShop_Id(currentUser.getId(), shop.getId());
        if (existing.isPresent()) return toConversation(existing.get(), null, 0);
        try {
            Long id = conversationCreator.create(currentUser, shop);
            return toConversation(loadConversation(id), null, 0);
        } catch (DataIntegrityViolationException race) {
            ChatConversation winner = conversationRepository
                    .findByCustomer_IdAndShop_Id(currentUser.getId(), shop.getId())
                    .orElseThrow(() -> race);
            return toConversation(winner, null, 0);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatConversationResponse> getCustomerConversations(AppUser user, int page, int size) {
        validatePage(page, size);
        return mapConversationPage(conversationRepository.findCustomerConversations(
                user.getId(), PageRequest.of(page, size)), user.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatConversationResponse> getSellerConversations(AppUser seller, int page, int size) {
        validatePage(page, size);
        Shop shop = ownedShop(seller);
        return mapConversationPage(conversationRepository.findShopConversations(
                shop.getId(), PageRequest.of(page, size)), seller.getId());
    }

    @Transactional(readOnly = true)
    public ChatConversationResponse getConversation(AppUser user, Long id) {
        ChatConversation conversation = authorizedConversation(user, id);
        List<ChatMessage> latest = messageRepository.findLatestForConversations(List.of(id));
        long unread = messageRepository.countUnreadByConversationIds(List.of(id), user.getId()).stream()
                .mapToLong(ConversationUnreadCountProjection::getUnreadCount).sum();
        return toConversation(conversation, latest.isEmpty() ? null : latest.get(0), unread);
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatMessageResponse> getMessages(AppUser user, Long id, int page, int size) {
        validatePage(page, size);
        authorizedConversation(user, id);
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        Page<ChatMessage> result = messageRepository.findByConversation_Id(id, PageRequest.of(page, size, sort));
        List<ChatMessageResponse> content = result.getContent().stream()
                .map(message -> toMessage(message, user.getId())).toList();
        return pageResponse(result, content);
    }

    @Transactional
    public ChatMessageResponse sendMessage(AppUser user, Long id, SendChatMessageRequest request) {
        ChatConversation conversation = authorizedConversation(user, id);
        ensureShopCanSend(conversation.getShop());
        String content = request.content() == null ? "" : request.content().trim();
        if (content.isBlank()) throw new BadRequestException("Nội dung tin nhắn không được để trống.");
        if (content.length() > 2000) throw new BadRequestException("Nội dung tin nhắn không được vượt quá 2000 ký tự.");
        LocalDateTime now = LocalDateTime.now();
        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSender(user);
        message.setContent(content);
        message.setCreatedAt(now);
        ChatMessage saved = messageRepository.save(message);
        conversation.setLastMessageAt(now);
        conversationRepository.save(conversation);
        ChatMessageResponse response = toMessage(saved, user.getId());
        eventPublisher.publishEvent(new ChatMessageCreatedEvent(
                customerRoute(conversation), sellerRoute(conversation),
                new RealtimeChatMessageResponse(
                        saved.getId(), conversation.getId(), user.getId(), user.getFullName(),
                        saved.getContent(), saved.getCreatedAt(), saved.getReadAt()
                )
        ));
        return response;
    }

    @Transactional
    public void markRead(AppUser user, Long id) {
        ChatConversation conversation = authorizedConversation(user, id);
        LocalDateTime readAt = LocalDateTime.now();
        messageRepository.markCounterpartMessagesRead(id, user.getId(), readAt);
        eventPublisher.publishEvent(new ChatReadUpdatedEvent(
                id, customerRoute(conversation), sellerRoute(conversation), user.getId(), readAt
        ));
    }

    @Transactional(readOnly = true)
    public ChatUnreadCountResponse getCustomerUnreadCount(AppUser user) {
        return new ChatUnreadCountResponse(messageRepository.countCustomerUnread(user.getId()));
    }

    @Transactional(readOnly = true)
    public ChatUnreadCountResponse getSellerUnreadCount(AppUser seller) {
        Shop shop = ownedShop(seller);
        return new ChatUnreadCountResponse(messageRepository.countSellerUnread(shop.getId(), seller.getId()));
    }

    private ChatConversation authorizedConversation(AppUser user, Long id) {
        ChatConversation conversation = loadConversation(id);
        boolean customer = Objects.equals(conversation.getCustomer().getId(), user.getId());
        boolean owner = Objects.equals(conversation.getShop().getOwner().getId(), user.getId());
        if (!customer && !owner) throw new ChatConversationNotFoundException(id);
        return conversation;
    }

    private ChatConversation loadConversation(Long id) {
        return conversationRepository.findDetailedById(id)
                .orElseThrow(() -> new ChatConversationNotFoundException(id));
    }

    private Shop ownedShop(AppUser seller) {
        return shopRepository.findByOwnerId(seller.getId())
                .orElseThrow(() -> new ShopNotFoundException("Không tìm thấy shop của người bán."));
    }

    private void ensureShopCanSend(Shop shop) {
        if (shop.getStatus() != ShopStatus.APPROVED || shop.getOwner().getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Shop hiện không thể gửi hoặc nhận tin nhắn mới.");
        }
    }

    private ChatParticipantRoute customerRoute(ChatConversation conversation) {
        AppUser customer = conversation.getCustomer();
        return new ChatParticipantRoute(customer.getId(), customer.getUsername(), ChatRecipientContext.CUSTOMER);
    }

    private ChatParticipantRoute sellerRoute(ChatConversation conversation) {
        AppUser seller = conversation.getShop().getOwner();
        return new ChatParticipantRoute(seller.getId(), seller.getUsername(), ChatRecipientContext.SELLER);
    }

    private PageResponse<ChatConversationResponse> mapConversationPage(Page<ChatConversation> page, Long userId) {
        List<Long> ids = page.getContent().stream().map(ChatConversation::getId).toList();
        if (ids.isEmpty()) return pageResponse(page, List.of());
        Map<Long, ChatMessage> latest = messageRepository.findLatestForConversations(ids).stream()
                .collect(Collectors.toMap(m -> m.getConversation().getId(), Function.identity()));
        Map<Long, Long> unread = messageRepository.countUnreadByConversationIds(ids, userId).stream()
                .collect(Collectors.toMap(ConversationUnreadCountProjection::getConversationId,
                        ConversationUnreadCountProjection::getUnreadCount));
        List<ChatConversationResponse> content = page.getContent().stream()
                .map(c -> toConversation(c, latest.get(c.getId()), unread.getOrDefault(c.getId(), 0L))).toList();
        return pageResponse(page, content);
    }

    private ChatConversationResponse toConversation(ChatConversation c, ChatMessage latest, long unread) {
        return new ChatConversationResponse(c.getId(),
                new ChatParticipantResponse(c.getCustomer().getId(), c.getCustomer().getFullName(), c.getCustomer().getAvatarUrl()),
                new ChatShopResponse(c.getShop().getId(), c.getShop().getName(), c.getShop().getLogoUrl()),
                c.getCreatedAt(), c.getLastMessageAt(), latest == null ? null : latest.getContent(), unread);
    }

    private ChatMessageResponse toMessage(ChatMessage m, Long userId) {
        return new ChatMessageResponse(m.getId(), m.getConversation().getId(), m.getSender().getId(),
                m.getSender().getFullName(), m.getContent(), m.getCreatedAt(), m.getReadAt(),
                Objects.equals(m.getSender().getId(), userId));
    }

    private <E, R> PageResponse<R> pageResponse(Page<E> page, List<R> content) {
        return PageResponse.<R>builder().content(content).pageNumber(page.getNumber()).pageSize(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages()).last(page.isLast()).build();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("page phải >= 0 và size phải từ 1 đến 100.");
        }
    }
}
