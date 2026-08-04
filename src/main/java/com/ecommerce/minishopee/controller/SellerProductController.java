package com.ecommerce.minishopee.controller;

import com.ecommerce.minishopee.dto.ProductRequest;
import com.ecommerce.minishopee.dto.ProductResponse;
import com.ecommerce.minishopee.entity.AppUser;
import com.ecommerce.minishopee.service.SellerProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller/products")
public class SellerProductController {

    private final SellerProductService sellerProductService;

    public SellerProductController(SellerProductService sellerProductService) {
        this.sellerProductService = sellerProductService;
    }

    @GetMapping
    public List<ProductResponse> getMyProducts(@AuthenticationPrincipal AppUser user) {
        return sellerProductService.getMyProducts(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@AuthenticationPrincipal AppUser user, @Valid @RequestBody ProductRequest request) {
        return sellerProductService.createProduct(user, request);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser user,
            @Valid @RequestBody ProductRequest request
    ) {
        return sellerProductService.updateProduct(id, user, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id, @AuthenticationPrincipal AppUser user) {
        sellerProductService.deleteProduct(id, user);
    }

    @PatchMapping("/{id}/hide")
    public ProductResponse hideProduct(@PathVariable Long id, @AuthenticationPrincipal AppUser user) {
        return sellerProductService.hideProduct(id, user);
    }

    @PatchMapping("/{id}/show")
    public ProductResponse showProduct(@PathVariable Long id, @AuthenticationPrincipal AppUser user) {
        return sellerProductService.showProduct(id, user);
    }
}