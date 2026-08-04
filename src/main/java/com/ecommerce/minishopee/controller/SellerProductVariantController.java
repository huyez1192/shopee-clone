package com.ecommerce.minishopee.controller;

import com.ecommerce.minishopee.dto.ProductVariantRequest;
import com.ecommerce.minishopee.dto.ProductVariantResponse;
import com.ecommerce.minishopee.entity.AppUser;
import com.ecommerce.minishopee.service.SellerProductService;
import com.ecommerce.minishopee.service.SellerProductVariantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller/products/{productId}/variants")
public class SellerProductVariantController {

    private final SellerProductVariantService sellerProductVariantService;

    public SellerProductVariantController (SellerProductVariantService sellerProductVariantService) {
        this.sellerProductVariantService = sellerProductVariantService;
    }



    @GetMapping
    public List<ProductVariantResponse> getVariants(@PathVariable Long productId, @AuthenticationPrincipal AppUser user) {

        return sellerProductVariantService.getVariants(productId, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductVariantResponse createVariant(
            @PathVariable Long productId,
            @AuthenticationPrincipal AppUser user,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        return sellerProductVariantService.createVariant(productId, user, request);
    }

    @PutMapping("/{variantId}")
    public ProductVariantResponse updateVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @AuthenticationPrincipal AppUser user,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        return sellerProductVariantService.updateVariant(productId, variantId, user, request);
    }

    @PatchMapping("/{variantId}/hide")
    public ProductVariantResponse hideVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @AuthenticationPrincipal AppUser user
    ) {
        return sellerProductVariantService.hideVariant(productId, variantId, user);
    }

    @PatchMapping("/{variantId}/show")
    public ProductVariantResponse showVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @AuthenticationPrincipal AppUser user
    ) {
        return sellerProductVariantService.showVariant(productId, variantId, user);
    }

    @DeleteMapping("/{variantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @AuthenticationPrincipal AppUser user
    ) {
        sellerProductVariantService.deleteVariant(productId, variantId, user);
    }
}
