package com.ecommerce.minishopee.service;

import com.ecommerce.minishopee.dto.ProductResponse;
import com.ecommerce.minishopee.entity.Product;
import com.ecommerce.minishopee.entity.ProductVariant;
import com.ecommerce.minishopee.entity.ProductVariantStatus;
import com.ecommerce.minishopee.mapper.ProductMapper;
import com.ecommerce.minishopee.repository.ProductVariantRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProductResponseAssembler {

    /*
     * Public chỉ nhìn thấy SKU:    ACTIVE hoặc OUT_OF_STOCK.
     */
    private static final Set<ProductVariantStatus>
            PUBLIC_VARIANT_STATUSES = EnumSet.of(
                    ProductVariantStatus.ACTIVE,
                    ProductVariantStatus.OUT_OF_STOCK
            );

    /*
     * Seller nhìn thấy cả SKU đang ẩn, nhưng không nhìn SKU đã DELETED.
     */
    private static final Set<ProductVariantStatus>
            SELLER_VARIANT_STATUSES = EnumSet.of(
                    ProductVariantStatus.ACTIVE,
                    ProductVariantStatus.INACTIVE,
                    ProductVariantStatus.OUT_OF_STOCK
            );

    private final ProductVariantRepository productVariantRepository;

    public ProductResponseAssembler(ProductVariantRepository productVariantRepository) {
        this.productVariantRepository = productVariantRepository;
    }

    // =====================================================
    // PUBLIC RESPONSE
    // =====================================================

    public ProductResponse toPublicResponse(Product product) {
        return toResponse(product, PUBLIC_VARIANT_STATUSES);
    }

    public List<ProductResponse> toPublicResponses(List<Product> products) {
        return toResponses(products, PUBLIC_VARIANT_STATUSES);
    }

    // =====================================================
    // SELLER RESPONSE
    // =====================================================

    public ProductResponse toSellerResponse(Product product) {
        return toResponse(product, SELLER_VARIANT_STATUSES);
    }

    public List<ProductResponse> toSellerResponses(List<Product> products) {
        return toResponses(products, SELLER_VARIANT_STATUSES);
    }

    // =====================================================
    // INTERNAL
    // =====================================================

    private ProductResponse toResponse(Product product, Collection<ProductVariantStatus> statuses) {
        List<ProductVariant> variants = productVariantRepository.findAllByProduct_IdAndStatusIn(product.getId(), statuses);

        return buildResponse(product, variants);
    }

    private List<ProductResponse> toResponses(List<Product> products, Collection<ProductVariantStatus> statuses) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = products.stream()
                                        .map(Product::getId)
                                        .toList();


        //  Chỉ chạy một query để lấy SKU của toàn bộ Product.
        List<ProductVariant> variants = productVariantRepository
                        .findAllByProduct_IdInAndStatusIn(productIds, statuses);

        Map<Long, List<ProductVariant>> variantsByProductId =
                variants.stream()
                        .collect(
                                Collectors.groupingBy(
                                        variant ->
                                                variant
                                                        .getProduct()
                                                        .getId()
                                )
                        );

        return products.stream()
                .map(product ->
                        buildResponse(
                                product,
                                variantsByProductId
                                        .getOrDefault(
                                                product.getId(),
                                                List.of()
                                        )
                        )
                )
                .toList();
    }

    private ProductResponse buildResponse(Product product, List<ProductVariant> variants) {
        BigDecimal minPrice =
                variants.stream()
                        .map(ProductVariant::getPrice)
                        .filter(Objects::nonNull)
                        .min(BigDecimal::compareTo)
                        .orElse(null);

        BigDecimal maxPrice =
                variants.stream()
                        .map(ProductVariant::getPrice)
                        .filter(Objects::nonNull)
                        .max(BigDecimal::compareTo)
                        .orElse(null);

        long totalStock =
                variants.stream()
                        .map(ProductVariant::getStockQuantity)
                        .filter(Objects::nonNull)
                        .mapToLong(Integer::longValue)
                        .sum();

        return ProductMapper.toResponse(
                product,
                minPrice,
                maxPrice,
                totalStock,
                variants.size()
        );
    }
}