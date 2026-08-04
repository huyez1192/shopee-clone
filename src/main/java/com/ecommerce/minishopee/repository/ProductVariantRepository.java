package com.ecommerce.minishopee.repository;

import com.ecommerce.minishopee.entity.ProductVariant;
import com.ecommerce.minishopee.entity.ProductVariantStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findAllByProductIdAndStatusNot(Long productId, ProductVariantStatus status);

    Optional<ProductVariant> findByIdAndProductId(Long variantId, Long productId);

    Optional<ProductVariant> findBySkuCodeIgnoreCase(String skuCode);

    List<ProductVariant> findAllByProduct_IdAndStatusIn(Long productId, Collection<ProductVariantStatus> statuses);

    List<ProductVariant> findAllByProduct_IdInAndStatusIn(Collection<Long> productIds, Collection<ProductVariantStatus> statuses);

    boolean existsByProduct_IdAndStatusIn(Long productId, Collection<ProductVariantStatus> statuses);

    List<ProductVariant> findAllByProduct_IdAndStatusInOrderByPriceAsc(Long productId, Collection<ProductVariantStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT pv
        FROM ProductVariant pv
        WHERE pv.id = :variantId
        """)
    Optional<ProductVariant> findByIdForUpdate(@Param("variantId") Long variantId);
}
