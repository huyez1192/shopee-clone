package com.ecommerce.minishopee.controller;

import com.ecommerce.minishopee.dto.AddCartItemRequest;
import com.ecommerce.minishopee.dto.CartResponse;
import com.ecommerce.minishopee.dto.UpdateCartItemRequest;
import com.ecommerce.minishopee.entity.AppUser;
import com.ecommerce.minishopee.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal AppUser currentUser) {
        return cartService.getCart(currentUser);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse addItem(@AuthenticationPrincipal AppUser currentUser,
                                @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(currentUser, request);
    }

    @PutMapping("/items/{id}")
    public CartResponse updateItem(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return cartService.updateItem(id, currentUser, request);
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        cartService.deleteItem(id, currentUser);
    }

    @DeleteMapping("/items")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(@AuthenticationPrincipal AppUser currentUser) {
        cartService.clearCart(currentUser);
    }
}