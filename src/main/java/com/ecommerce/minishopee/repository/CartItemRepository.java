package com.ecommerce.minishopee.repository;

import com.ecommerce.minishopee.entity.CartItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @EntityGraph(attributePaths = {
                    "productVariant",
                    "productVariant.product",
                    "productVariant.product.shop",
                    "productVariant.product.shop.owner",
                    "productVariant.product.category"
                }
    )
    List<CartItem> findAllByCart_IdOrderByCreatedAtAsc(Long cartId);

    Optional<CartItem> findByCart_IdAndProductVariant_Id(Long cartId, Long variantId);

    @EntityGraph(attributePaths = {
                    "productVariant",
                    "productVariant.product",
                    "productVariant.product.shop",
                    "productVariant.product.shop.owner",
                    "productVariant.product.category"
                }
    )
    Optional<CartItem> findByIdAndCart_User_Id(Long cartItemId, Long userId);


    // Checkout Preview    → chỉ đọc
    @EntityGraph(
            attributePaths = {
                    "cart",
                    "cart.user",
                    "productVariant",
                    "productVariant.product",
                    "productVariant.product.shop",
                    "productVariant.product.shop.owner",
                    "productVariant.product.category"
            }
    )
    List<CartItem> findAllByIdInAndCart_User_Id(Collection<Long> cartItemIds, Long userId);

    void deleteAllByCart_Id(Long cartId);


    // Create Order   → đọc và khóa CartItem
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT ci
        FROM CartItem ci
        WHERE ci.id IN :cartItemIds
          AND ci.cart.user.id = :userId
        ORDER BY ci.id ASC
        """)
    List<CartItem> findAllOwnedByIdsForUpdate(@Param("cartItemIds") Collection<Long> cartItemIds,
                                              @Param("userId") Long userId);
}
