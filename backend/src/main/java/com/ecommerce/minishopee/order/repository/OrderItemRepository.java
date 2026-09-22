package com.ecommerce.minishopee.order.repository;

import com.ecommerce.minishopee.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("""
            SELECT oi FROM OrderItem oi
            JOIN FETCH oi.order o
            JOIN FETCH o.buyer
            JOIN FETCH oi.product
            WHERE oi.id = :orderItemId
            """)
    Optional<OrderItem> findDetailedById(@Param("orderItemId") Long orderItemId);
}
