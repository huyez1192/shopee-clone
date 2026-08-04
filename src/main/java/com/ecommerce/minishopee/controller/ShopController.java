package com.ecommerce.minishopee.controller;

import com.ecommerce.minishopee.dto.ShopRegisterRequest;
import com.ecommerce.minishopee.dto.ShopResponse;
import com.ecommerce.minishopee.dto.ShopUpdateRequest;
import com.ecommerce.minishopee.entity.AppUser;
import com.ecommerce.minishopee.service.ShopService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    /*
     * User đăng ký mở shop.
     */
    @PostMapping("/api/shops/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ShopResponse registerShop(@AuthenticationPrincipal AppUser user, @Valid @RequestBody ShopRegisterRequest request) {
        return shopService.registerShop(user, request);
    }

    /*
     * User/Seller xem shop của chính mình.
     */
    @GetMapping("/api/seller/shop")
    public ShopResponse getMyShop(@AuthenticationPrincipal AppUser user) {
        return shopService.getMyShop(user);
    }

    /*
     * Seller cập nhật shop của mình.
     */
    @PutMapping("/api/seller/shop")
    public ShopResponse updateMyShop(@AuthenticationPrincipal AppUser user, @Valid @RequestBody ShopUpdateRequest request) {
        return shopService.updateMyShop(user, request);
    }
}