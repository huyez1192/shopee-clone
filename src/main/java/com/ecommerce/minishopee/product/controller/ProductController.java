package com.ecommerce.minishopee.product.controller;

import com.ecommerce.minishopee.common.dto.PageResponse;
import com.ecommerce.minishopee.product.dto.response.ProductResponse;
import com.ecommerce.minishopee.product.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }


    @GetMapping("/search")
    public List<ProductResponse> searchProductsByName(@RequestParam String name) {
        return productService.searchProductsByName(name);
    }

    @GetMapping("/filter/category")
    public List<ProductResponse> filterProductByCategory(@RequestParam Long categoryId) {
        return productService.filterProductsByCategory(categoryId);
    }

    @GetMapping("/filter/price")
    public List<ProductResponse> filterProductsByPrice(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice
    ) {
        return productService.filterProductsByPrice(minPrice, maxPrice);
    }

    @GetMapping("/search-filter")
    public List<ProductResponse> searchAndFilterProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice
    ) {
        return productService.searchAndFilterProducts(name, categoryId, minPrice, maxPrice);
    }


    @GetMapping("/page")
    public PageResponse<ProductResponse> getProductsWithPaginationAndSorting(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return productService.getProductsWithPaginationAndSorting(page, size, sortBy, direction);
    }





}