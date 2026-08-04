package com.ecommerce.minishopee.product.service;

import com.ecommerce.minishopee.common.dto.PageResponse;
import com.ecommerce.minishopee.product.dto.response.ProductResponse;
import com.ecommerce.minishopee.category.enums.CategoryStatus;
import com.ecommerce.minishopee.product.entity.Product;
import com.ecommerce.minishopee.product.enums.ProductStatus;
import com.ecommerce.minishopee.product.enums.ProductVariantStatus;
import com.ecommerce.minishopee.product.assembler.ProductResponseAssembler;
import com.ecommerce.minishopee.shop.enums.ShopStatus;
import com.ecommerce.minishopee.user.enums.UserStatus;
import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.product.exception.ProductNotFoundException;
import com.ecommerce.minishopee.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class ProductService {

    private static final Set<ProductVariantStatus> PUBLIC_VARIANT_STATUSES = EnumSet.of(
                    ProductVariantStatus.ACTIVE,
                    ProductVariantStatus.OUT_OF_STOCK
            );
    private static final Set<String> ALLOWED_PRODUCT_SORT_FIELDS = Set.of(
                    "name",
                    "createdAt",
                    "updatedAt"
            );
    private final ProductRepository productRepository;
    private final ProductResponseAssembler productResponseAssembler;

    public ProductService(ProductRepository productRepository, ProductResponseAssembler productResponseAssembler) {
        this.productRepository = productRepository;
        this.productResponseAssembler = productResponseAssembler;
    }

    // =====================================================
    // GET ALL
    // =====================================================

    public List<ProductResponse> getAllProducts() {

        List<Product> products = productRepository.findAllPublicProducts(
                                ProductStatus.ACTIVE,
                                ShopStatus.APPROVED,
                                UserStatus.ACTIVE,
                                CategoryStatus.ACTIVE,
                                PUBLIC_VARIANT_STATUSES
        );

        return productResponseAssembler.toPublicResponses(products);
    }

    // =====================================================
    // GET BY ID
    // =====================================================

    public ProductResponse getProductById(Long productId) {
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

        return productResponseAssembler.toPublicResponse(product);
    }

    // =====================================================
    // SEARCH NAME
    // =====================================================

    public List<ProductResponse> searchProductsByName(String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }

        return searchAndFilterProducts(name, null, null, null);
    }

    // =====================================================
    // FILTER CATEGORY
    // =====================================================

    public List<ProductResponse> filterProductsByCategory(Long categoryId) {
        if (categoryId == null) {
            throw new BadRequestException("categoryId không được để trống.");
        }

        return searchAndFilterProducts(null, categoryId, null, null);
    }

    // =====================================================
    // FILTER PRICE
    // =====================================================

    public List<ProductResponse> filterProductsByPrice(BigDecimal minPrice, BigDecimal maxPrice) {
        validatePriceRange(minPrice, maxPrice);

        return searchAndFilterProducts(null, null, minPrice, maxPrice);
    }

    // =====================================================
    // SEARCH + FILTER
    // =====================================================

    public List<ProductResponse> searchAndFilterProducts(
            String name,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        validatePriceRange(minPrice, maxPrice);

        String normalizedName = name == null || name.isBlank()
                ? ""
                : name.trim().toLowerCase();

        List<Product> products = productRepository.searchPublicProducts(
                                    normalizedName,
                                    categoryId,
                                    minPrice,
                                    maxPrice,
                                    ProductStatus.ACTIVE,
                                    ShopStatus.APPROVED,
                                    UserStatus.ACTIVE,
                                    CategoryStatus.ACTIVE,
                                    PUBLIC_VARIANT_STATUSES
                                );

        return productResponseAssembler.toPublicResponses(products);
    }

    // =====================================================
    // PAGINATION + SORT
    // =====================================================

    public PageResponse<ProductResponse> getProductsWithPaginationAndSorting(
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        validatePageRequest(page, size);

        String normalizedSortBy =
                sortBy == null || sortBy.isBlank()
                        ? "createdAt"
                        : sortBy.trim();

        String normalizedDirection =
                direction == null
                        ? "desc"
                        : direction.trim();

        /*
         * Product không còn field price.
         * Sort theo price được thực hiện sau khi tính minPrice.
         */
        if (normalizedSortBy.equalsIgnoreCase("price")
                || normalizedSortBy.equalsIgnoreCase("minPrice")) {

            return getProductsSortedByPrice(page, size, normalizedDirection);
        }

        if (!ALLOWED_PRODUCT_SORT_FIELDS.contains(normalizedSortBy)) {
            throw new BadRequestException("sortBy chỉ chấp nhận: " + "name, createdAt, updatedAt hoặc price.");
        }

        Sort sort = normalizedDirection.equalsIgnoreCase("desc")
                        ? Sort.by(normalizedSortBy).descending()
                        : Sort.by(normalizedSortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> productPage = productRepository.findPublicProductsPage(
                                        ProductStatus.ACTIVE,
                                        ShopStatus.APPROVED,
                                        UserStatus.ACTIVE,
                                        CategoryStatus.ACTIVE,
                                        PUBLIC_VARIANT_STATUSES,
                                        pageable
                                    );

        List<ProductResponse> content = productResponseAssembler.toPublicResponses(productPage.getContent());

        return PageResponse
                .<ProductResponse>builder()
                .content(content)
                .pageNumber(productPage.getNumber())
                .pageSize(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .last(productPage.isLast())
                .build();
    }

    /*
     * Phiên bản hiện tại lấy toàn bộ Product,
     * tính minPrice rồi mới phân trang.
     *
     * Phù hợp giai đoạn học và dữ liệu nhỏ.
     * Sau này có thể tối ưu bằng native query/projection.
     */
    private PageResponse<ProductResponse> getProductsSortedByPrice(int page, int size, String direction) {
        List<ProductResponse> products = new ArrayList<>(getAllProducts());

        Comparator<BigDecimal> priceComparator;

        if (direction.equalsIgnoreCase("desc")) {
            priceComparator = Comparator.nullsLast(Comparator.reverseOrder());
        } else {
            priceComparator = Comparator.nullsLast(Comparator.naturalOrder());
        }

        products.sort(Comparator.comparing(ProductResponse::getMinPrice, priceComparator));

        int totalElements = products.size();

        int totalPages = totalElements == 0
                        ? 0
                        : (int) Math.ceil((double) totalElements / size);

        int fromIndex = Math.min(page * size, totalElements);

        int toIndex = Math.min(fromIndex + size, totalElements);

        List<ProductResponse> content = new ArrayList<>(products.subList(fromIndex, toIndex));

        boolean last = totalPages == 0 || page >= totalPages - 1;

        return PageResponse
                .<ProductResponse>builder()
                .content(content)
                .pageNumber(page)
                .pageSize(size)
                .totalElements((long) totalElements)
                .totalPages(totalPages)
                .last(last)
                .build();
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("minPrice không được nhỏ hơn 0.");
        }

        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("maxPrice không được nhỏ hơn 0.");
        }

        if (minPrice != null
                && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {

            throw new BadRequestException("minPrice phải nhỏ hơn hoặc bằng maxPrice.");
        }
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("page phải lớn hơn hoặc bằng 0.");
        }

        if (size <= 0) {
            throw new BadRequestException("size phải lớn hơn 0.");
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().toLowerCase();
    }
}