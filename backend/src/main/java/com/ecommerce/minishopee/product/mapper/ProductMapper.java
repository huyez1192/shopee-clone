package com.ecommerce.minishopee.product.mapper;

import com.ecommerce.minishopee.product.dto.request.ProductRequest;
import com.ecommerce.minishopee.product.dto.response.ProductResponse;
import com.ecommerce.minishopee.category.entity.Category;
import com.ecommerce.minishopee.product.entity.Product;
import com.ecommerce.minishopee.product.enums.ProductStatus;
import com.ecommerce.minishopee.shop.entity.Shop;

import java.math.BigDecimal;

public class ProductMapper {

    public static Product toEntity(ProductRequest request, Category category, Shop shop) {
        Product product = new Product();

        product.setName(request.getName().trim());
        product.setDescription(normalize(request.getDescription()));
        product.setCategory(category);
        product.setShop(shop);
        product.setImageUrl(normalize(request.getImageUrl()));
        product.setStatus(ProductStatus.INACTIVE);

        return product;
    }

    public static ProductResponse toResponse(Product product,
                                             BigDecimal minPrice,
                                             BigDecimal maxPrice,
                                             Long totalStock,
                                             Integer variantCount,
                                             double averageRating,
                                             long reviewCount) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .categorySlug(product.getCategory().getSlug())
                .shopId(product.getShop().getId())
                .shopName(product.getShop().getName())
                .status(product.getStatus())
                .imageUrl(product.getImageUrl())
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .totalStock(totalStock)
                .variantCount(variantCount)
                .averageRating(averageRating)
                .reviewCount(reviewCount)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public static void updateEntity(Product product, ProductRequest request, Category category) {
        product.setName(request.getName().trim());
        product.setDescription(normalize(request.getDescription()));
        product.setCategory(category);
        product.setImageUrl(normalize(request.getImageUrl()));
    }


    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
