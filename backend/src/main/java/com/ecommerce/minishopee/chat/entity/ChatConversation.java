package com.ecommerce.minishopee.chat.entity;

import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "chat_conversations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_conversations_customer_shop",
                columnNames = {"customer_id", "shop_id"}
        ),
        indexes = {
                @Index(name = "idx_chat_conversations_customer_activity", columnList = "customer_id, last_message_at"),
                @Index(name = "idx_chat_conversations_shop_activity", columnList = "shop_id, last_message_at")
        }
)
public class ChatConversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private AppUser customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
