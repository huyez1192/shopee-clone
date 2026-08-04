package com.ecommerce.minishopee.mapper;

import com.ecommerce.minishopee.dto.CartItemResponse;
import com.ecommerce.minishopee.entity.CartItem;
import com.ecommerce.minishopee.entity.CartItemAvailability;
import com.ecommerce.minishopee.entity.Product;
import com.ecommerce.minishopee.entity.ProductVariant;

import java.math.BigDecimal;

public class CartMapper {

    private CartMapper() {
    }

    public static CartItemResponse toItemResponse (
            CartItem item,
            CartItemAvailability availability,
            String unavailableReason) {

        ProductVariant variant = item.getProductVariant();

        Product product = variant.getProduct();

        BigDecimal unitPrice = variant.getPrice();

        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

        boolean available = availability == CartItemAvailability.AVAILABLE;

        return CartItemResponse.builder()
                .id(item.getId())
                .variantId(variant.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productImageUrl(product.getImageUrl())
                .shopId(product.getShop().getId())
                .shopName(product.getShop().getName())
                .variantName1(variant.getVariantName1())
                .variantValue1(variant.getVariantValue1())
                .variantName2(variant.getVariantName2())
                .variantValue2(variant.getVariantValue2())
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .availableStock(variant.getStockQuantity())
                .lineTotal(lineTotal)
                .variantStatus(variant.getStatus())
                .available(available)
                .availabilityStatus(availability)
                .unavailableReason(unavailableReason)
                .build();

    }
}
