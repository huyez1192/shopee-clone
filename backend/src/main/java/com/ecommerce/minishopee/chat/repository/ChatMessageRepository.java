package com.ecommerce.minishopee.chat.repository;

import com.ecommerce.minishopee.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @EntityGraph(attributePaths = "sender")
    Page<ChatMessage> findByConversation_Id(Long conversationId, Pageable pageable);

    @EntityGraph(attributePaths = "sender")
    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.conversation.id IN :conversationIds
              AND NOT EXISTS (
                  SELECT newer.id FROM ChatMessage newer
                  WHERE newer.conversation = m.conversation
                    AND (newer.createdAt > m.createdAt
                         OR (newer.createdAt = m.createdAt AND newer.id > m.id))
              )
            """)
    List<ChatMessage> findLatestForConversations(
            @Param("conversationIds") Collection<Long> conversationIds
    );

    @Query("""
            SELECT m.conversation.id AS conversationId, COUNT(m.id) AS unreadCount
            FROM ChatMessage m
            WHERE m.conversation.id IN :conversationIds
              AND m.sender.id <> :currentUserId
              AND m.readAt IS NULL
            GROUP BY m.conversation.id
            """)
    List<ConversationUnreadCountProjection> countUnreadByConversationIds(
            @Param("conversationIds") Collection<Long> conversationIds,
            @Param("currentUserId") Long currentUserId
    );

    @Query("""
            SELECT COUNT(m.id) FROM ChatMessage m
            WHERE m.conversation.customer.id = :customerId
              AND m.sender.id <> :customerId
              AND m.readAt IS NULL
            """)
    long countCustomerUnread(@Param("customerId") Long customerId);

    @Query("""
            SELECT COUNT(m.id) FROM ChatMessage m
            WHERE m.conversation.shop.id = :shopId
              AND m.sender.id <> :sellerId
              AND m.readAt IS NULL
            """)
    long countSellerUnread(@Param("shopId") Long shopId, @Param("sellerId") Long sellerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ChatMessage m SET m.readAt = :readAt
            WHERE m.conversation.id = :conversationId
              AND m.sender.id <> :currentUserId
              AND m.readAt IS NULL
            """)
    int markCounterpartMessagesRead(
            @Param("conversationId") Long conversationId,
            @Param("currentUserId") Long currentUserId,
            @Param("readAt") LocalDateTime readAt
    );
}
