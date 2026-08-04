package com.ecommerce.minishopee.product.mapper;

import com.ecommerce.minishopee.product.dto.response.PublicProductVariantResponse;
import com.ecommerce.minishopee.product.entity.ProductVariant;

public class PublicProductVariantMapper {

    private PublicProductVariantMapper() {
    }

    public static PublicProductVariantResponse toResponse(ProductVariant variant) {
        return PublicProductVariantResponse.builder()
                .id(variant.getId())
                .productId(variant.getProduct().getId())
                .variantName1(variant.getVariantName1())
                .variantValue1(variant.getVariantValue1())
                .variantName2(variant.getVariantName2())
                .variantValue2(variant.getVariantValue2())
                .price(variant.getPrice())
                .stockQuantity(variant.getStockQuantity())
                .status(variant.getStatus())
                .build();
    }
}