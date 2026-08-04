package com.ecommerce.minishopee.service;

import com.ecommerce.minishopee.dto.ProductResponse;
import com.ecommerce.minishopee.dto.ProductVariantRequest;
import com.ecommerce.minishopee.dto.ProductVariantResponse;
import com.ecommerce.minishopee.entity.*;
import com.ecommerce.minishopee.exception.BadRequestException;
import com.ecommerce.minishopee.exception.ProductNotFoundException;
import com.ecommerce.minishopee.exception.ProductVariantNotFoundException;
import com.ecommerce.minishopee.exception.ShopNotFoundException;
import com.ecommerce.minishopee.mapper.ProductVariantMapper;
import com.ecommerce.minishopee.repository.ProductRepository;
import com.ecommerce.minishopee.repository.ProductVariantRepository;
import com.ecommerce.minishopee.repository.ShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LongSummaryStatistics;

@Service
public class SellerProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;

    public SellerProductVariantService(
                ProductVariantRepository productVariantRepository,
                ProductRepository productRepository,
                ShopRepository shopRepository
        ) {
        this.productVariantRepository = productVariantRepository;
        this.productRepository = productRepository;
        this.shopRepository = shopRepository;
    }


    // Seller xem tất cả SKU của Product thuộc shop mình.
    public List<ProductVariantResponse> getVariants(Long productId, AppUser currentUser) {
        Product product = getSellerProduct(productId, currentUser);

        return productVariantRepository.findAllByProductIdAndStatusNot(product.getId(), ProductVariantStatus.DELETED)
                .stream()
                .map(ProductVariantMapper :: toResponse)
                .toList();
    }

    // Seller tạo SKU.
    @Transactional
    public ProductVariantResponse createVariant(Long productId, AppUser currentUser, ProductVariantRequest request) {
        Product product = getSellerProduct(productId, currentUser);

        validateSkuCode(request.getSkuCode(), null);

        ProductVariant variant = ProductVariantMapper.toEntity(request, product);

        ProductVariant savedVariant = productVariantRepository.save(variant);

        return ProductVariantMapper.toResponse(savedVariant);
    }

    // Seller sửa SKU.
    @Transactional
    public ProductVariantResponse updateVariant (
            Long productId,
            Long variantId,
            AppUser currentUser,
            ProductVariantRequest request
    ) {
        Product product = getSellerProduct(productId, currentUser);

        ProductVariant variant = findVariant(variantId, product.getId());

        validateSkuCode(request.getSkuCode(), variantId);

        ProductVariantMapper.updateEntity(variant, request);

        //    Nếu tồn kho về 0:
        //    → OUT_OF_STOCK
        //
        //    Nếu SKU trước đó OUT_OF_STOCK và được nhập thêm hàng:
        //    → ACTIVE
        //
        //    Nếu đang INACTIVE thì giữ INACTIVE.

        if (request.getStockQuantity() == 0) {
            variant.setStatus(ProductVariantStatus.OUT_OF_STOCK);
        }

        else if (variant.getStatus() == ProductVariantStatus.OUT_OF_STOCK) {
            variant.setStatus(ProductVariantStatus.ACTIVE);
        }

        ProductVariant updatedVariant = productVariantRepository.save(variant);

        return ProductVariantMapper.toResponse(updatedVariant);
    }

    // Seller ẩn SKU.
    @Transactional
    public ProductVariantResponse hideVariant (Long productId, Long variantId, AppUser currentUser) {
        Product product = getSellerProduct(productId, currentUser);

        ProductVariant variant = findVariant(variantId, product.getId());

        variant.setStatus(ProductVariantStatus.INACTIVE);

        return ProductVariantMapper.toResponse(productVariantRepository.save(variant));
    }

    // Seller hiện lại SKU.
    @Transactional
    public ProductVariantResponse showVariant (Long productId, Long variantId, AppUser currentUser) {
        Product product = getSellerProduct(productId, currentUser);

        ProductVariant variant = findVariant(variantId, product.getId());

        if (variant.getStockQuantity() == 0) {
            variant.setStatus(ProductVariantStatus.OUT_OF_STOCK);
        } else {
            variant.setStatus(ProductVariantStatus.ACTIVE);
        }

        return ProductVariantMapper.toResponse(productVariantRepository.save(variant));
    }

    // Soft delete SKU.
    @Transactional
    public void deleteVariant (Long productId, Long variantId, AppUser currentUser) {
        Product product = getSellerProduct(productId, currentUser);

        ProductVariant variant = findVariant(variantId, product.getId());

        variant.setStatus(ProductVariantStatus.DELETED);

        productVariantRepository.save(variant);
    }





    /*
     * Kiểm tra:
     *
     * Seller
     * → Shop APPROVED
     * → Product thuộc Shop
     */
    private Product getSellerProduct(Long productId, AppUser currentUser) {
        Shop shop = shopRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ShopNotFoundException("Bạn chưa có shop."));

        if (shop.getStatus() != ShopStatus.APPROVED) {
            throw new BadRequestException("Shop chưa được duyệt hoặc hiện không hoạt động.");
        }

        Product product= productRepository.findByIdAndShopId(productId, shop.getId())
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (product.getStatus() == ProductStatus.DELETED) {
            throw new ProductVariantNotFoundException(productId);
        }

        return product;
    }


    // Kiểm tra SKU thuộc Product.
    private ProductVariant findVariant(Long variantId, Long productId) {
        ProductVariant variant = productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ProductVariantNotFoundException(variantId));

        return variant;
    }


    // SKU code nếu có thì không được trùng.
    private void validateSkuCode(String skuCode, Long currentVariantId) {
        if (skuCode == null || skuCode.isBlank()) {
            return;
        }

        String normalizedSkuCode = skuCode.trim().toUpperCase();

        productVariantRepository.findBySkuCodeIgnoreCase(normalizedSkuCode)
                .filter(existingVariant -> currentVariantId == null || !existingVariant.getId().equals(currentVariantId))
                .ifPresent(existingVariant -> {
                    throw new BadRequestException("Mã SKU đã tồn tại.");
                });
    }

}
