package com.ecommerce.minishopee.controller;

import com.ecommerce.minishopee.dto.PublicProductVariantResponse;
import com.ecommerce.minishopee.service.PublicProductVariantService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/variants")
public class PublicProductVariantController {

    private final PublicProductVariantService publicProductVariantService;

    public PublicProductVariantController(PublicProductVariantService publicProductVariantService) {
        this.publicProductVariantService = publicProductVariantService;
    }

    @GetMapping
    public List<PublicProductVariantResponse> getPublicVariants(@PathVariable Long productId) {
        return publicProductVariantService.getPublicVariants(productId);
    }
}