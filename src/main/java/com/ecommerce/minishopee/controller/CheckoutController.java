package com.ecommerce.minishopee.controller;

import com.ecommerce.minishopee.dto.CheckoutPreviewRequest;
import com.ecommerce.minishopee.dto.CheckoutPreviewResponse;
import com.ecommerce.minishopee.entity.AppUser;
import com.ecommerce.minishopee.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/preview")
    public CheckoutPreviewResponse preview(
            @AuthenticationPrincipal AppUser currentUser,
            @Valid @RequestBody CheckoutPreviewRequest request
    ) {
        return checkoutService.preview(currentUser, request);
    }
}