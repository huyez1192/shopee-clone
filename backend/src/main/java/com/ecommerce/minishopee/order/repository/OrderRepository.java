package com.ecommerce.minishopee.order.repository;

import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.order.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import com.ecommerce.minishopee.statistics.projection.OrderFinancialSummaryProjection;
import com.ecommerce.minishopee.statistics.projection.OrderPeriodProjection;
import com.ecommerce.minishopee.statistics.projection.OrderStatusCountProjection;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            SELECT COALESCE(SUM(o.merchandiseSubtotal), 0) AS completedRevenue,
                   COALESCE(SUM(o.totalAmount), 0) AS completedGmv,
                   COUNT(o.id) AS completedOrderCount
            FROM Order o
            WHERE (:shopId IS NULL OR o.shop.id = :shopId)
              AND o.status = com.ecommerce.minishopee.order.enums.OrderStatus.COMPLETED
              AND o.paymentStatus = com.ecommerce.minishopee.order.enums.PaymentStatus.PAID
              AND o.completedAt >= :from AND o.completedAt < :toExclusive
            """)
    OrderFinancialSummaryProjection summarizeCompletedOrders(
            @Param("shopId") Long shopId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive
    );

    @Query("""
            SELECT COUNT(o.id) FROM Order o
            WHERE (:shopId IS NULL OR o.shop.id = :shopId)
              AND o.createdAt >= :from AND o.createdAt < :toExclusive
            """)
    long countCreatedOrders(@Param("shopId") Long shopId,
                            @Param("from") LocalDateTime from,
                            @Param("toExclusive") LocalDateTime toExclusive);

    @Query("""
            SELECT o.status AS status, COUNT(o.id) AS orderCount
            FROM Order o
            WHERE (:shopId IS NULL OR o.shop.id = :shopId)
              AND o.createdAt >= :from AND o.createdAt < :toExclusive
            GROUP BY o.status
            """)
    List<OrderStatusCountProjection> countByCurrentStatus(
            @Param("shopId") Long shopId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive
    );

    @Query("""
            SELECT year(o.completedAt) AS periodYear, month(o.completedAt) AS periodMonth,
                   day(o.completedAt) AS periodDay,
                   SUM(o.merchandiseSubtotal) AS completedRevenue,
                   SUM(o.totalAmount) AS completedGmv,
                   COUNT(o.id) AS completedOrderCount
            FROM Order o
            WHERE o.shop.id = :shopId
              AND o.status = com.ecommerce.minishopee.order.enums.OrderStatus.COMPLETED
              AND o.paymentStatus = com.ecommerce.minishopee.order.enums.PaymentStatus.PAID
              AND o.completedAt >= :from AND o.completedAt < :toExclusive
            GROUP BY year(o.completedAt), month(o.completedAt), day(o.completedAt)
            ORDER BY year(o.completedAt), month(o.completedAt), day(o.completedAt)
            """)
    List<OrderPeriodProjection> summarizeCompletedOrdersByDay(
            @Param("shopId") Long shopId, @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    @Query("""
            SELECT year(o.completedAt) AS periodYear, month(o.completedAt) AS periodMonth,
                   0 AS periodDay,
                   SUM(o.merchandiseSubtotal) AS completedRevenue,
                   SUM(o.totalAmount) AS completedGmv,
                   COUNT(o.id) AS completedOrderCount
            FROM Order o
            WHERE o.shop.id = :shopId
              AND o.status = com.ecommerce.minishopee.order.enums.OrderStatus.COMPLETED
              AND o.paymentStatus = com.ecommerce.minishopee.order.enums.PaymentStatus.PAID
              AND o.completedAt >= :from AND o.completedAt < :toExclusive
            GROUP BY year(o.completedAt), month(o.completedAt)
            ORDER BY year(o.completedAt), month(o.completedAt)
            """)
    List<OrderPeriodProjection> summarizeCompletedOrdersByMonth(
            @Param("shopId") Long shopId, @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o
            FROM Order o
            WHERE o.payment.id = :paymentId
            ORDER BY o.id ASC
            """)
    List<Order> findAllByPaymentIdForUpdateOrdered(@Param("paymentId") Long paymentId);

    // =====================================================
    // BUYER ORDER
    // =====================================================

    /*
     * Buyer xem toàn bộ đơn hàng của mình.
     *
     * Fetch luôn Shop, Buyer, OrderItem, Product và ProductVariant.
     */
    @Query("""
            SELECT DISTINCT o
            FROM Order o
            JOIN FETCH o.buyer b
            JOIN FETCH o.shop s
            LEFT JOIN FETCH o.items oi
            LEFT JOIN FETCH oi.product
            LEFT JOIN FETCH oi.productVariant
            WHERE b.id = :buyerId
            ORDER BY o.createdAt DESC
            """)
    List<Order> findAllDetailedByBuyerId(@Param("buyerId") Long buyerId);


    // Buyer lọc đơn theo trạng thái.
    @Query("""
            SELECT DISTINCT o
            FROM Order o
            JOIN FETCH o.buyer b
            JOIN FETCH o.shop s
            LEFT JOIN FETCH o.items oi
            LEFT JOIN FETCH oi.product
            LEFT JOIN FETCH oi.productVariant
            WHERE b.id = :buyerId
              AND o.status = :status
            ORDER BY o.createdAt DESC
            """)
    List<Order> findAllDetailedByBuyerIdAndStatus(
            @Param("buyerId") Long buyerId,
            @Param("status") OrderStatus status
    );


    // Buyer xem chi tiết đơn của mình.
    @Query("""
            SELECT DISTINCT o
            FROM Order o
            JOIN FETCH o.buyer b
            JOIN FETCH o.shop s
            LEFT JOIN FETCH o.items oi
            LEFT JOIN FETCH oi.product
            LEFT JOIN FETCH oi.productVariant
            WHERE o.id = :orderId
              AND b.id = :buyerId
            """)
    Optional<Order> findDetailedByIdAndBuyerId(
            @Param("orderId") Long orderId,
            @Param("buyerId") Long buyerId
    );

    /*
     * Khóa Order khi hủy để tránh hai request cùng hủy một đơn và hoàn tồn kho hai lần.
     *
     * Không fetch join ở query khóa.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o
            FROM Order o
            WHERE o.id = :orderId
              AND o.buyer.id = :buyerId
            """)
    Optional<Order> findOwnedOrderByIdForUpdate(
            @Param("orderId") Long orderId,
            @Param("buyerId") Long buyerId
    );


    // =====================================================
    // SELLER ORDER
    // =====================================================

    // Seller xem toàn bộ Order của shop mình.
    @Query("""
            SELECT DISTINCT o
            FROM Order o
            JOIN FETCH o.buyer b
            JOIN FETCH o.shop s
            LEFT JOIN FETCH o.items oi
            LEFT JOIN FETCH oi.product
            LEFT JOIN FETCH oi.productVariant
            WHERE s.id = :shopId
            ORDER BY o.createdAt DESC
            """)
    List<Order> findAllDetailedByShopId(@Param("shopId") Long shopId);

    // Seller lọc Order theo trạng thái.
    @Query("""
            SELECT DISTINCT o
            FROM Order o
            JOIN FETCH o.buyer b
            JOIN FETCH o.shop s
            LEFT JOIN FETCH o.items oi
            LEFT JOIN FETCH oi.product
            LEFT JOIN FETCH oi.productVariant
            WHERE s.id = :shopId
              AND o.status = :status
            ORDER BY o.createdAt DESC
            """)
    List<Order> findAllDetailedByShopIdAndStatus(
            @Param("shopId") Long shopId,
            @Param("status") OrderStatus status
    );


    // Seller xem chi tiết Order thuộc shop mình.
    @Query("""
            SELECT DISTINCT o
            FROM Order o
            JOIN FETCH o.buyer b
            JOIN FETCH o.shop s
            LEFT JOIN FETCH o.items oi
            LEFT JOIN FETCH oi.product
            LEFT JOIN FETCH oi.productVariant
            WHERE o.id = :orderId
              AND s.id = :shopId
            """)
    Optional<Order> findDetailedByIdAndShopId(
            @Param("orderId") Long orderId,
            @Param("shopId") Long shopId
    );

    /*
     * Khóa Order khi Seller cập nhật trạng thái.
     * Điều kiện shopId đồng thời kiểm tra ownership.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o
            FROM Order o
            WHERE o.id = :orderId
              AND o.shop.id = :shopId
            """)
    Optional<Order> findSellerOrderByIdForUpdate(
            @Param("orderId") Long orderId,
            @Param("shopId") Long shopId
    );


    // =====================================================
    // ADMIN ORDER
    // =====================================================

    /*
     * Admin xem, tìm kiếm và lọc toàn bộ đơn hàng.
     *
     * keyword: tìm theo orderCode.
     *
     * Các tham số còn lại có thể null.
     */
    @Query("""
        SELECT DISTINCT o
        FROM Order o
        JOIN FETCH o.buyer b
        JOIN FETCH o.shop s
        LEFT JOIN FETCH o.items oi
        LEFT JOIN FETCH oi.product
        LEFT JOIN FETCH oi.productVariant
        WHERE LOWER(o.orderCode)
              LIKE CONCAT('%', :keyword, '%')
          AND (
                :status IS NULL
                OR o.status = :status
              )
          AND (
                :buyerId IS NULL
                OR b.id = :buyerId
              )
          AND (
                :shopId IS NULL
                OR s.id = :shopId
              )
        ORDER BY o.createdAt DESC
        """)
    List<Order> searchDetailedForAdmin(
            @Param("keyword") String keyword,
            @Param("status") OrderStatus status,
            @Param("buyerId") Long buyerId,
            @Param("shopId") Long shopId
    );

    // Admin xem chi tiết bất kỳ Order nào.
    @Query("""
        SELECT DISTINCT o
        FROM Order o
        JOIN FETCH o.buyer b
        JOIN FETCH o.shop s
        LEFT JOIN FETCH o.items oi
        LEFT JOIN FETCH oi.product
        LEFT JOIN FETCH oi.productVariant
        WHERE o.id = :orderId
        """)
    Optional<Order> findDetailedByIdForAdmin(
            @Param("orderId") Long orderId
    );

    // Khóa Order khi Admin thay đổi trạng thái hoặc hủy đơn.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT o
        FROM Order o
        WHERE o.id = :orderId
        """)
    Optional<Order> findAdminOrderByIdForUpdate(
            @Param("orderId") Long orderId
    );
}
