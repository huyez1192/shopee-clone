package com.ecommerce.minishopee.dto;

import com.ecommerce.minishopee.entity.ProductVariantStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ProductVariantResponse {

    private Long id;

    private Long productId;

    private String productName;

    private String variantName1;

    private String variantValue1;

    private String variantName2;

    private String variantValue2;

    private BigDecimal price;

    private Integer stockQuantity;

    private String skuCode;

    private ProductVariantStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}