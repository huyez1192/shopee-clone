package com.ecommerce.minishopee.order.checkout.controller;

import com.ecommerce.minishopee.order.checkout.dto.request.CheckoutPreviewRequest;
import com.ecommerce.minishopee.order.checkout.dto.response.CheckoutPreviewResponse;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.order.checkout.service.CheckoutService;
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