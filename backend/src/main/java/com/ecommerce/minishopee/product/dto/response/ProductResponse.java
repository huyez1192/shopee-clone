package com.ecommerce.minishopee.product.dto.response;

import com.ecommerce.minishopee.product.enums.ProductStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@Builder
public class ProductResponse {
    private Long id;

    private String name;

    private String description;

    private Long categoryId;

    private String categoryName;

    private String categorySlug;

    private Long shopId;

    private String shopName;

    private ProductStatus status;

    private String imageUrl;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private Long totalStock;

    private Integer variantCount;

    private Double averageRating;

    private Long reviewCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
