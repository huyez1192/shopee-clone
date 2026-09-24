package com.ecommerce.minishopee.order.repository;

import com.ecommerce.minishopee.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import com.ecommerce.minishopee.statistics.projection.TopProductProjection;
import com.ecommerce.minishopee.statistics.projection.UnitsPeriodProjection;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("""
            SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi
            WHERE (:shopId IS NULL OR oi.order.shop.id = :shopId)
              AND oi.order.status = com.ecommerce.minishopee.order.enums.OrderStatus.COMPLETED
              AND oi.order.paymentStatus = com.ecommerce.minishopee.order.enums.PaymentStatus.PAID
              AND oi.order.completedAt >= :from AND oi.order.completedAt < :toExclusive
            """)
    long sumUnitsSold(@Param("shopId") Long shopId, @Param("from") LocalDateTime from,
                      @Param("toExclusive") LocalDateTime toExclusive);

    @Query("""
            SELECT oi.product.id AS productId,
                   MAX(oi.productNameSnapshot) AS productName,
                   MAX(oi.productImageUrlSnapshot) AS imageUrl,
                   SUM(oi.quantity) AS unitsSold,
                   SUM(oi.lineTotal) AS merchandiseValue
            FROM OrderItem oi
            WHERE oi.order.shop.id = :shopId
              AND oi.order.status = com.ecommerce.minishopee.order.enums.OrderStatus.COMPLETED
              AND oi.order.paymentStatus = com.ecommerce.minishopee.order.enums.PaymentStatus.PAID
              AND oi.order.completedAt >= :from AND oi.order.completedAt < :toExclusive
            GROUP BY oi.product.id
            ORDER BY SUM(oi.quantity) DESC, SUM(oi.lineTotal) DESC, oi.product.id ASC
            """)
    List<TopProductProjection> findTopProducts(
            @Param("shopId") Long shopId, @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive, Pageable pageable);

    @Query("""
            SELECT year(oi.order.completedAt) AS periodYear,
                   month(oi.order.completedAt) AS periodMonth,
                   day(oi.order.completedAt) AS periodDay,
                   SUM(oi.quantity) AS unitsSold
            FROM OrderItem oi
            WHERE oi.order.shop.id = :shopId
              AND oi.order.status = com.ecommerce.minishopee.order.enums.OrderStatus.COMPLETED
              AND oi.order.paymentStatus = com.ecommerce.minishopee.order.enums.PaymentStatus.PAID
              AND oi.order.completedAt >= :from AND oi.order.completedAt < :toExclusive
            GROUP BY year(oi.order.completedAt), month(oi.order.completedAt), day(oi.order.completedAt)
            """)
    List<UnitsPeriodProjection> sumUnitsSoldByDay(
            @Param("shopId") Long shopId, @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    @Query("""
            SELECT year(oi.order.completedAt) AS periodYear,
                   month(oi.order.completedAt) AS periodMonth,
                   0 AS periodDay, SUM(oi.quantity) AS unitsSold
            FROM OrderItem oi
            WHERE oi.order.shop.id = :shopId
              AND oi.order.status = com.ecommerce.minishopee.order.enums.OrderStatus.COMPLETED
              AND oi.order.paymentStatus = com.ecommerce.minishopee.order.enums.PaymentStatus.PAID
              AND oi.order.completedAt >= :from AND oi.order.completedAt < :toExclusive
            GROUP BY year(oi.order.completedAt), month(oi.order.completedAt)
            """)
    List<UnitsPeriodProjection> sumUnitsSoldByMonth(
            @Param("shopId") Long shopId, @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);
    @Query("""
            SELECT oi FROM OrderItem oi
            JOIN FETCH oi.order o
            JOIN FETCH o.buyer
            JOIN FETCH oi.product
            WHERE oi.id = :orderItemId
            """)
    Optional<OrderItem> findDetailedById(@Param("orderItemId") Long orderItemId);
}
