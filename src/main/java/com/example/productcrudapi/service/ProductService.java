package com.example.productcrudapi.service;

import com.example.productcrudapi.dto.PageResponse;
import com.example.productcrudapi.dto.ProductRequest;
import com.example.productcrudapi.dto.ProductResponse;
import com.example.productcrudapi.entity.Product;
import com.example.productcrudapi.exception.BadRequestException;
import com.example.productcrudapi.exception.ProductNotFoundException;
import com.example.productcrudapi.mapper.ProductMapper;
import com.example.productcrudapi.repository.ProductRepository;
import org.springframework.boot.data.autoconfigure.web.DataWebProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> getAllProducts() {
        List<Product> products = productRepository.findAll();

        return products.stream()
                .map(ProductMapper::toResponse)
                .toList();

        // Bằng 3 dòng trên
//        List<ProductResponse> responses = new ArrayList<>();
//        for (Product product : products) {
//            ProductResponse response = ProductMapper.toResponse(product);
//            responses.add(response);
//        }
//        return responses;
    }

    public ProductResponse createProduct(ProductRequest request) {
        Product product = ProductMapper.toEntity(request);

        Product savedProduct = productRepository.save(product);

        return ProductMapper.toResponse(savedProduct);
    }

    public ProductResponse getProductById (Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return ProductMapper.toResponse(product);
    }

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        ProductMapper.updateEntity(product, request);

        Product updatedProduct = productRepository.save(product);

        return ProductMapper.toResponse(updatedProduct);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }

        productRepository.deleteById(id);
    }

    public List<ProductResponse> searchProductsByName(String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }

        List<Product> products = productRepository.findByNameContainingIgnoreCase(name.trim());

        return products.stream()
                .map(ProductMapper :: toResponse)
                .toList();
    }

    public List<ProductResponse> searchProductsByCategory(String category) {
        if (category == null || category.isBlank()) {
            return List.of();
        }

        List<Product> products = productRepository.findByCategoryIgnoreCase(category.trim());

        return products.stream()
                .map(ProductMapper :: toResponse)
                .toList();
    }

    public List<ProductResponse> filterProductsByPrice(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice phải <= maxPrice");
        }

        List<Product> products = productRepository.findByPriceBetween(minPrice, maxPrice);

        return products.stream()
                .map(ProductMapper :: toResponse)
                .toList();
    }

    public PageResponse<ProductResponse> getProductsWithPaginationAndSorting(int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> productPage = productRepository.findAll(pageable);

        List<ProductResponse> content = productPage.getContent()
                .stream()
                .map(ProductMapper :: toResponse)
                .toList();

        return PageResponse.<ProductResponse>builder()
                .content(content)
                .pageNumber(productPage.getNumber())
                .pageSize(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .last(productPage.isLast())
                .build();
    }

    public List<ProductResponse> searchAndFilterProducts(String name, String category, BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice phải <= maxPrice");
        }

        String normalizedName = name == null || name.isBlank() ? null : name.trim();
        String normalizedCategory = category == null || category.isBlank() ? null : category.trim();

        List<Product> products = productRepository.searchAndFilterProducts(normalizedName, normalizedCategory, minPrice, maxPrice);

        return products.stream()
                .map(ProductMapper :: toResponse)
                .toList();
    }
}
