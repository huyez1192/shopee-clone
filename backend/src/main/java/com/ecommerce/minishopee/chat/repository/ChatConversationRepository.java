package com.ecommerce.minishopee.chat.repository;

import com.ecommerce.minishopee.chat.entity.ChatConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    @EntityGraph(attributePaths = {"customer", "shop", "shop.owner"})
    Optional<ChatConversation> findByCustomer_IdAndShop_Id(Long customerId, Long shopId);

    @EntityGraph(attributePaths = {"customer", "shop", "shop.owner"})
    @Query("""
            SELECT c FROM ChatConversation c
            WHERE c.customer.id = :customerId
            ORDER BY COALESCE(c.lastMessageAt, c.createdAt) DESC, c.id DESC
            """)
    Page<ChatConversation> findCustomerConversations(
            @Param("customerId") Long customerId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"customer", "shop", "shop.owner"})
    @Query("""
            SELECT c FROM ChatConversation c
            WHERE c.shop.id = :shopId
            ORDER BY COALESCE(c.lastMessageAt, c.createdAt) DESC, c.id DESC
            """)
    Page<ChatConversation> findShopConversations(@Param("shopId") Long shopId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "shop", "shop.owner"})
    @Query("SELECT c FROM ChatConversation c WHERE c.id = :id")
    Optional<ChatConversation> findDetailedById(@Param("id") Long id);
}
