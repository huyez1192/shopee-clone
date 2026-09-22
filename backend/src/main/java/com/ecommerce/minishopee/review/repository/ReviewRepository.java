package com.ecommerce.minishopee.review.repository;

import com.ecommerce.minishopee.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByOrderItem_Id(Long orderItemId);

    Optional<Review> findByOrderItem_Id(Long orderItemId);

    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.buyer
            JOIN FETCH r.product
            JOIN FETCH r.orderItem
            WHERE r.product.id = :productId
            """)
    Page<Review> findDetailedByProductId(@Param("productId") Long productId, Pageable pageable);

    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.buyer
            JOIN FETCH r.product p
            JOIN FETCH r.orderItem oi
            WHERE p.shop.id = :shopId
              AND (:replied IS NULL OR (:replied = TRUE AND r.sellerReply IS NOT NULL)
                   OR (:replied = FALSE AND r.sellerReply IS NULL))
              AND (:rating IS NULL OR r.rating = :rating)
            """)
    Page<Review> findDetailedByShopId(
            @Param("shopId") Long shopId,
            @Param("replied") Boolean replied,
            @Param("rating") Integer rating,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.product p
            JOIN FETCH p.shop
            JOIN FETCH r.buyer
            JOIN FETCH r.orderItem
            WHERE r.id = :reviewId
            """)
    Optional<Review> findDetailedByIdForUpdate(@Param("reviewId") Long reviewId);

    @Query("""
            SELECT r.product.id AS productId,
                   AVG(r.rating) AS averageRating,
                   COUNT(r.id) AS reviewCount
            FROM Review r
            WHERE r.product.id IN :productIds
            GROUP BY r.product.id
            """)
    List<ProductRatingProjection> summarizeByProductIds(@Param("productIds") Collection<Long> productIds);

    @Query("""
            SELECT r.orderItem.id AS orderItemId, r.id AS reviewId
            FROM Review r
            WHERE r.orderItem.id IN :orderItemIds
            """)
    List<OrderItemReviewProjection> findReviewIdsByOrderItemIds(
            @Param("orderItemIds") Collection<Long> orderItemIds
    );
}
