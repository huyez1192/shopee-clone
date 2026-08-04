package com.ecommerce.minishopee.controller;

import com.ecommerce.minishopee.dto.CancelOrderRequest;
import com.ecommerce.minishopee.dto.CreateOrderRequest;
import com.ecommerce.minishopee.dto.CreateOrderResponse;
import com.ecommerce.minishopee.dto.OrderResponse;
import com.ecommerce.minishopee.entity.AppUser;
import com.ecommerce.minishopee.entity.OrderStatus;
import com.ecommerce.minishopee.service.BuyerOrderService;
import com.ecommerce.minishopee.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final BuyerOrderService buyerOrderService;

    public OrderController(OrderService orderService, BuyerOrderService buyerOrderService) {
        this.orderService = orderService;
        this.buyerOrderService = buyerOrderService;
    }

    // Buyer tạo Order COD.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateOrderResponse createOrders(
            @AuthenticationPrincipal AppUser currentUser,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return orderService.createOrders(currentUser, request);
    }

    /*
     * Buyer xem lịch sử đơn hàng.
     *
     * Có thể lọc:   GET /api/orders?status=PENDING
     */
    @GetMapping
    public List<OrderResponse> getMyOrders(
            @AuthenticationPrincipal AppUser currentUser,
            @RequestParam(required = false) OrderStatus status
    ) {
        return buyerOrderService.getMyOrders(currentUser, status);
    }

    // Buyer xem chi tiết đơn hàng của mình.
    @GetMapping("/{id}")
    public OrderResponse getMyOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        return buyerOrderService.getMyOrderById(id, currentUser);
    }


    // Buyer hủy đơn hàng của mình.
    @PatchMapping("/{id}/cancel")
    public OrderResponse cancelMyOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        return buyerOrderService.cancelMyOrder(id, currentUser, request);
    }
}