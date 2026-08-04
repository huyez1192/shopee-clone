package com.ecommerce.minishopee.dto;

import com.ecommerce.minishopee.entity.ProductVariantStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class PublicProductVariantResponse {

    private Long id;

    private Long productId;

    private String variantName1;

    private String variantValue1;

    private String variantName2;

    private String variantValue2;

    private BigDecimal price;

    private Integer stockQuantity;

    private ProductVariantStatus status;
}