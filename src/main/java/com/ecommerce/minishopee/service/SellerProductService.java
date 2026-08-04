package com.ecommerce.minishopee.service;

import com.ecommerce.minishopee.category.entity.Category;
import com.ecommerce.minishopee.category.enums.CategoryStatus;
import com.ecommerce.minishopee.dto.ProductRequest;
import com.ecommerce.minishopee.dto.ProductResponse;
import com.ecommerce.minishopee.entity.*;
import com.ecommerce.minishopee.exception.BadRequestException;
import com.ecommerce.minishopee.category.exception.CategoryNotFoundException;
import com.ecommerce.minishopee.exception.ProductNotFoundException;
import com.ecommerce.minishopee.exception.ShopNotFoundException;
import com.ecommerce.minishopee.mapper.ProductMapper;
import com.ecommerce.minishopee.category.repository.CategoryRepository;
import com.ecommerce.minishopee.repository.ProductRepository;
import com.ecommerce.minishopee.repository.ProductVariantRepository;
import com.ecommerce.minishopee.repository.ShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class SellerProductService {

    private static final Set<ProductVariantStatus> SHOWABLE_VARIANT_STATUSES =
            EnumSet.of(
                    ProductVariantStatus.ACTIVE,
                    ProductVariantStatus.OUT_OF_STOCK
            );
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ShopRepository shopRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductResponseAssembler productResponseAssembler;

    public SellerProductService(ProductRepository productRepository,
                                CategoryRepository categoryRepository,
                                ShopRepository shopRepository,
                                ProductVariantRepository productVariantRepository,
                                ProductResponseAssembler productResponseAssembler) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.shopRepository = shopRepository;
        this.productVariantRepository = productVariantRepository;
        this.productResponseAssembler = productResponseAssembler;
    }

    // Seller xem sản phẩm của shop mình.
    public List<ProductResponse> getMyProducts(AppUser currentUser) {
        Shop shop = getApprovedShop(currentUser);

        List<Product> products = productRepository.findAllByShopIdAndStatusNot(shop.getId(), ProductStatus.DELETED);

        return productResponseAssembler.toSellerResponses(products);
    }



    // Seller tạo Product cho shop của chính mình.
    @Transactional
    public ProductResponse createProduct(AppUser currentUser, ProductRequest request) {
        Shop shop = getApprovedShop(currentUser);

        Category category = getActiveCategory(request.getCategoryId());

        Product product = ProductMapper.toEntity(request, category, shop);

        Product savedProduct = productRepository.save(product);

        return productResponseAssembler.toSellerResponse(savedProduct);
    }


    // Seller cập nhật Product.
    //
    // Product bắt buộc phải thuộc shop của seller.
    @Transactional
    public ProductResponse updateProduct(Long productId, AppUser currentUser, ProductRequest request) {
        Shop shop = getApprovedShop(currentUser);

        Product product = findSellerProduct(productId, shop.getId());

        Category category = getActiveCategory(request.getCategoryId());

        ProductMapper.updateEntity(product, request, category);

        Product updatedProduct = productRepository.save(product);

        return productResponseAssembler.toSellerResponse(updatedProduct);
    }


    // Soft delete Product.
    @Transactional
    public void deleteProduct(Long productId, AppUser currentUser) {

        Shop shop = getApprovedShop(currentUser);

        Product product = findSellerProduct(productId, shop.getId());

        product.setStatus(ProductStatus.DELETED);

        productRepository.save(product);
    }


    // Seller ẩn Product.
    @Transactional
    public ProductResponse hideProduct(Long productId, AppUser currentUser) {

        Shop shop = getApprovedShop(currentUser);

        Product product = findSellerProduct(productId, shop.getId());

        product.setStatus(ProductStatus.INACTIVE);

        Product savedProduct = productRepository.save(product);

        return productResponseAssembler.toSellerResponse(savedProduct);
    }


    // Seller hiện lại Product.
    @Transactional
    public ProductResponse showProduct(Long productId, AppUser currentUser) {

        Shop shop = getApprovedShop(currentUser);

        Product product = findSellerProduct(productId, shop.getId());

        boolean hasValidVariant = productVariantRepository.existsByProduct_IdAndStatusIn(
                                            product.getId(),
                                            SHOWABLE_VARIANT_STATUSES
                                    );

        if (!hasValidVariant) {
            throw new BadRequestException("Sản phẩm phải có ít nhất một SKU trước khi hiển thị.");
        }

        product.setStatus(ProductStatus.ACTIVE);

        Product savedProduct = productRepository.save(product);

        return productResponseAssembler.toSellerResponse(savedProduct);
    }




    // Lấy category đang ACTIVE
    private Category getActiveCategory(Long categoryId) {
        return categoryRepository.findByIdAndStatus(categoryId, CategoryStatus.ACTIVE)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }


    // Lấy shop của Seller và kiểm tra shop phải APPROVED.
    private Shop getApprovedShop(AppUser currentUser) {
        Shop shop = shopRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ShopNotFoundException("Bạn chưa có shop."));

        if (shop.getStatus() != ShopStatus.APPROVED) {
            throw new BadRequestException("Shop chưa được duyệt hoặc không hoạt động.");
        }

        return shop;

    }


    // Kiểm tra Product có thuộc shop Seller hay không.
    private Product findSellerProduct(Long productId, Long shopId) {
        Product product = productRepository.findByIdAndShopId(productId, shopId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (product.getStatus() == ProductStatus.DELETED) {
            throw new ProductNotFoundException(productId);
        }

        return product;
    }


}
