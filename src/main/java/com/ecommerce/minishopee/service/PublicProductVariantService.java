package com.ecommerce.minishopee.service;

import com.ecommerce.minishopee.category.enums.CategoryStatus;
import com.ecommerce.minishopee.dto.PublicProductVariantResponse;
import com.ecommerce.minishopee.entity.*;
import com.ecommerce.minishopee.exception.ProductNotFoundException;
import com.ecommerce.minishopee.mapper.PublicProductVariantMapper;
import com.ecommerce.minishopee.repository.ProductRepository;
import com.ecommerce.minishopee.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class PublicProductVariantService {

    private static final Set<ProductVariantStatus> PUBLIC_VARIANT_STATUSES =
            EnumSet.of(
                    ProductVariantStatus.ACTIVE,
                    ProductVariantStatus.OUT_OF_STOCK
            );
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    public PublicProductVariantService(ProductRepository productRepository, ProductVariantRepository productVariantRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @Transactional(readOnly = true)
    public List<PublicProductVariantResponse> getPublicVariants(Long productId) {
        /*
         * Không chỉ kiểm tra Product tồn tại.
         *
         * Product phải đồng thời:
         * - ACTIVE
         * - Shop APPROVED
         * - Category ACTIVE
         * - Có SKU public hợp lệ
         */
        Product product = productRepository
                        .findPublicProductById(
                                productId,
                                ProductStatus.ACTIVE,
                                ShopStatus.APPROVED,
                                UserStatus.ACTIVE,
                                CategoryStatus.ACTIVE,
                                PUBLIC_VARIANT_STATUSES
                        )
                        .orElseThrow(() -> new ProductNotFoundException(productId));

        List<ProductVariant> variants = productVariantRepository
                        .findAllByProduct_IdAndStatusInOrderByPriceAsc(
                                product.getId(),
                                PUBLIC_VARIANT_STATUSES
                        );

        return variants.stream()
                .map(PublicProductVariantMapper::toResponse)
                .toList();
    }
}