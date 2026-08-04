package com.ecommerce.minishopee.product.repository;

import com.ecommerce.minishopee.category.enums.CategoryStatus;
import com.ecommerce.minishopee.product.entity.Product;
import com.ecommerce.minishopee.product.enums.ProductStatus;
import com.ecommerce.minishopee.product.enums.ProductVariantStatus;
import com.ecommerce.minishopee.shop.enums.ShopStatus;
import com.ecommerce.minishopee.user.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

    // =====================================================
    // PUBLIC PRODUCT
    // =====================================================

    /*
     * Chỉ lấy Product:
     *
     * Product ACTIVE
     * Shop APPROVED
     * Category ACTIVE
     * Có ít nhất một SKU được hiển thị
     */
    @Query("""
            SELECT p
            FROM Product p
            WHERE p.status = :productStatus
              AND p.shop.status = :shopStatus
              AND p.shop.owner.status = :ownerStatus
              AND p.category.status = :categoryStatus
              AND EXISTS (
                    SELECT pv.id
                    FROM ProductVariant pv
                    WHERE pv.product = p
                      AND pv.status IN :variantStatuses
              )
            """)
    List<Product> findAllPublicProducts(
            @Param("productStatus") ProductStatus productStatus,
            @Param("shopStatus") ShopStatus shopStatus,
            @Param("ownerStatus") UserStatus ownerStatus,
            @Param("categoryStatus") CategoryStatus categoryStatus,
            @Param("variantStatuses") Collection<ProductVariantStatus> variantStatuses
    );

    /*
     * Public xem chi tiết Product.
     */
    @Query("""
            SELECT p
            FROM Product p
            WHERE p.id = :productId
              AND p.status = :productStatus
              AND p.shop.status = :shopStatus
              AND p.shop.owner.status = :ownerStatus
              AND p.category.status = :categoryStatus
              AND EXISTS (
                    SELECT pv.id
                    FROM ProductVariant pv
                    WHERE pv.product = p
                      AND pv.status IN :variantStatuses
              )
            """)
    Optional<Product> findPublicProductById(
            @Param("productId") Long productId,
            @Param("productStatus") ProductStatus productStatus,
            @Param("shopStatus") ShopStatus shopStatus,
            @Param("ownerStatus") UserStatus ownerStatus,
            @Param("categoryStatus") CategoryStatus categoryStatus,
            @Param("variantStatuses") Collection<ProductVariantStatus> variantStatuses
    );

    /*
     * Search và filter.
     *
     * Giá được kiểm tra bằng ProductVariant.price,
     * không dùng Product.price.
     */
    @Query("""
        SELECT DISTINCT p
        FROM Product p
        WHERE p.status = :productStatus
          AND p.shop.status = :shopStatus
          AND p.shop.owner.status = :ownerStatus
          AND p.category.status = :categoryStatus
          AND LOWER(p.name) LIKE CONCAT('%', :name, '%')
          AND (:categoryId IS NULL
               OR p.category.id = :categoryId)
          AND EXISTS (
              SELECT pv.id
              FROM ProductVariant pv
              WHERE pv.product.id = p.id
                AND pv.status IN :variantStatuses
                AND (:minPrice IS NULL OR pv.price >= :minPrice)
                AND (:maxPrice IS NULL OR pv.price <= :maxPrice)
          )
        """)
    List<Product> searchPublicProducts(
            @Param("name") String name,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("productStatus") ProductStatus productStatus,
            @Param("shopStatus") ShopStatus shopStatus,
            @Param("ownerStatus") UserStatus ownerStatus,
            @Param("categoryStatus") CategoryStatus categoryStatus,
            @Param("variantStatuses") Collection<ProductVariantStatus> variantStatuses
    );

    /*
     * Phân trang public.
     *
     * Dùng cho sort theo name, createdAt, updatedAt.
     * Sort theo price sẽ xử lý riêng trong ProductService.
     */
    @Query(
            value = """
                    SELECT p
                    FROM Product p
                    WHERE p.status = :productStatus
                      AND p.shop.status = :shopStatus
                      AND p.shop.owner.status = :ownerStatus                    
                      AND p.category.status = :categoryStatus
                      AND EXISTS (
                            SELECT pv.id
                            FROM ProductVariant pv
                            WHERE pv.product = p
                              AND pv.status IN :variantStatuses
                      )
                    """,
            countQuery = """
                    SELECT COUNT(p)
                    FROM Product p
                    WHERE p.status = :productStatus
                      AND p.shop.status = :shopStatus
                      AND p.shop.owner.status = :ownerStatus
                      AND p.category.status = :categoryStatus
                      AND EXISTS (
                            SELECT pv.id
                            FROM ProductVariant pv
                            WHERE pv.product = p
                              AND pv.status IN :variantStatuses
                      )
                    """
    )
    Page<Product> findPublicProductsPage(
            @Param("productStatus") ProductStatus productStatus,
            @Param("shopStatus") ShopStatus shopStatus,
            @Param("ownerStatus") UserStatus ownerStatus,
            @Param("categoryStatus") CategoryStatus categoryStatus,
            @Param("variantStatuses") Collection<ProductVariantStatus> variantStatuses,

            Pageable pageable
    );

    // =====================================================
    // SELLER PRODUCT
    // =====================================================

    /*
     * Ownership:
     * Product phải thuộc Shop của Seller.
     */
    Optional<Product> findByIdAndShopId(Long productId, Long shopId);

    /*
     * Seller xem Product của Shop mình,
     * không lấy Product đã DELETED.
     */
    List<Product> findAllByShopIdAndStatusNot(Long shopId, ProductStatus status);
}