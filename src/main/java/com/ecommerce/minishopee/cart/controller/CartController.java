package com.ecommerce.minishopee.cart.controller;

import com.ecommerce.minishopee.cart.dto.request.AddCartItemRequest;
import com.ecommerce.minishopee.cart.dto.response.CartResponse;
import com.ecommerce.minishopee.cart.dto.request.UpdateCartItemRequest;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.cart.service.CartService;
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