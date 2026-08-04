package com.ecommerce.minishopee.product.mapper;

import com.ecommerce.minishopee.product.dto.request.ProductVariantRequest;
import com.ecommerce.minishopee.product.dto.response.ProductVariantResponse;
import com.ecommerce.minishopee.product.entity.Product;
import com.ecommerce.minishopee.product.entity.ProductVariant;
import com.ecommerce.minishopee.product.enums.ProductVariantStatus;

public class ProductVariantMapper {

    public static ProductVariant toEntity(ProductVariantRequest request, Product product) {

        ProductVariant variant = new ProductVariant();

        variant.setProduct(product);

        variant.setVariantName1(normalize(request.getVariantName1()));
        variant.setVariantValue1(normalize(request.getVariantValue1()));

        variant.setVariantName2(normalize(request.getVariantName2()));
        variant.setVariantValue2(normalize(request.getVariantValue2()));

        variant.setPrice(request.getPrice());

        variant.setStockQuantity(request.getStockQuantity());

        variant.setSkuCode(normalizeSkuCode(request.getSkuCode()));

        if (request.getStockQuantity() == 0) {
            variant.setStatus(ProductVariantStatus.OUT_OF_STOCK);
        } else {
            variant.setStatus(ProductVariantStatus.ACTIVE);
        }

        return variant;
    }

    public static void updateEntity(ProductVariant variant, ProductVariantRequest request) {

        variant.setVariantName1(normalize(request.getVariantName1()));
        variant.setVariantValue1(normalize(request.getVariantValue1()));

        variant.setVariantName2(normalize(request.getVariantName2()));
        variant.setVariantValue2(normalize(request.getVariantValue2()));

        variant.setPrice(request.getPrice());

        variant.setStockQuantity(request.getStockQuantity());

        variant.setSkuCode(normalizeSkuCode(request.getSkuCode()));
    }

    public static ProductVariantResponse toResponse(ProductVariant variant) {

        return ProductVariantResponse.builder()
                .id(variant.getId())
                .productId(variant.getProduct().getId())
                .productName(variant.getProduct().getName())
                .variantName1(variant.getVariantName1())
                .variantValue1(variant.getVariantValue1())
                .variantName2(variant.getVariantName2())
                .variantValue2(variant.getVariantValue2())
                .price(variant.getPrice())
                .stockQuantity(variant.getStockQuantity())
                .skuCode(variant.getSkuCode())
                .status(variant.getStatus())
                .createdAt(variant.getCreatedAt())
                .updatedAt(variant.getUpdatedAt())
                .build();
    }

    private static String normalize(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private static String normalizeSkuCode(String skuCode) {

        if (skuCode == null || skuCode.isBlank()) {
            return null;
        }

        return skuCode
                .trim()
                .toUpperCase();
    }
}