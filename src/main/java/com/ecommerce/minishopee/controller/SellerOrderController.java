package com.ecommerce.minishopee.controller;

import com.ecommerce.minishopee.dto.OrderResponse;
import com.ecommerce.minishopee.dto.SellerCancelOrderRequest;
import com.ecommerce.minishopee.entity.AppUser;
import com.ecommerce.minishopee.entity.OrderStatus;
import com.ecommerce.minishopee.service.SellerOrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller/orders")
public class SellerOrderController {

    private final SellerOrderService sellerOrderService;

    public SellerOrderController(SellerOrderService sellerOrderService) {
        this.sellerOrderService = sellerOrderService;
    }

    /*
     * GET /api/seller/orders
     *
     * Hoặc:
     * GET /api/seller/orders?status=PENDING
     */
    @GetMapping
    public List<OrderResponse> getShopOrders(
            @AuthenticationPrincipal AppUser currentUser,
            @RequestParam(required = false) OrderStatus status
    ) {
        return sellerOrderService.getShopOrders(currentUser, status);
    }

    // Seller xem chi tiết Order của shop mình.
    @GetMapping("/{id}")
    public OrderResponse getShopOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        return sellerOrderService.getShopOrderById(id, currentUser);
    }

    // PENDING → CONFIRMED
    @PatchMapping("/{id}/confirm")
    public OrderResponse confirmOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        return sellerOrderService.confirmOrder(id, currentUser);
    }

    // CONFIRMED → SHIPPING
    @PatchMapping("/{id}/ship")
    public OrderResponse shipOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        return sellerOrderService.shipOrder(id, currentUser);
    }

    // SHIPPING → COMPLETED
    @PatchMapping("/{id}/complete")
    public OrderResponse completeOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        return sellerOrderService.completeOrder(id, currentUser);
    }

    // Seller hủy Order.
    @PatchMapping("/{id}/cancel")
    public OrderResponse cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser,
            @Valid @RequestBody SellerCancelOrderRequest request
    ) {
        return sellerOrderService.cancelOrder(id, currentUser, request);
    }
}