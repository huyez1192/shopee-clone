package com.example.productcrudapi.controller;

import com.example.productcrudapi.dto.PageResponse;
import com.example.productcrudapi.dto.ProductRequest;
import com.example.productcrudapi.dto.ProductResponse;
import com.example.productcrudapi.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct (@Valid @RequestBody ProductRequest request) {
        return productService.createProduct(request);
    }

    @GetMapping("/search")
    public List<ProductResponse> searchProductsByName(@RequestParam String name) {
        return productService.searchProductsByName(name);
    }

    @GetMapping("/filter/category")
    public List<ProductResponse> searchProductByCategory(@RequestParam String category) {
        return productService.searchProductsByCategory(category);
    }

    @GetMapping("/filter/price")
    public List<ProductResponse> filterProductsByPrice(@RequestParam BigDecimal minPrice, @RequestParam BigDecimal maxPrice) {
        return productService.filterProductsByPrice(minPrice, maxPrice);
    }

    @GetMapping("/page")
    public PageResponse<ProductResponse> getProductsWithPaginationAndSorting(@RequestParam(defaultValue = "0") int page,
                                                                             @RequestParam(defaultValue = "5") int size,
                                                                             @RequestParam(defaultValue = "id") String sortBy,
                                                                             @RequestParam(defaultValue = "asc") String direction) {
        return productService.getProductsWithPaginationAndSorting(page, size, sortBy, direction);
    }

    @GetMapping("/search-filter")
    public List<ProductResponse> searchAndFilterProducts (@RequestParam(required = false) String name,
                                                          @RequestParam(required = false) String category,
                                                          @RequestParam(required = false) BigDecimal minPrice,
                                                          @RequestParam(required = false) BigDecimal maxPrice) {
        return productService.searchAndFilterProducts(name, category, minPrice, maxPrice);
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
    }

}