package com.ecommerce.minishopee.order.admin.controller;

import com.ecommerce.minishopee.order.admin.dto.request.AdminCancelOrderRequest;
import com.ecommerce.minishopee.order.admin.dto.request.AdminUpdateOrderStatusRequest;
import com.ecommerce.minishopee.order.dto.response.OrderResponse;
import com.ecommerce.minishopee.order.enums.OrderStatus;
import com.ecommerce.minishopee.order.admin.service.AdminOrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    /*
     * GET /api/admin/orders
     *
     * Các filter đều không bắt buộc.
     */
    @GetMapping
    public List<OrderResponse> getAllOrders(

            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long buyerId,
            @RequestParam(required = false) Long shopId
    ) {
        return adminOrderService.getAllOrders(keyword, status, buyerId, shopId);
    }

    // Admin xem chi tiết một Order.
    @GetMapping("/{id}")
    public OrderResponse getOrderById(@PathVariable Long id) {
        return adminOrderService.getOrderById(id);
    }

    // Admin cập nhật trạng thái Order.
    @PatchMapping("/{id}/status")
    public OrderResponse updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateOrderStatusRequest request
    ) {
        return adminOrderService.updateOrderStatus(id, request);
    }

    // Admin hủy Order.
    @PatchMapping("/{id}/cancel")
    public OrderResponse cancelOrder(
            @PathVariable Long id,
            @Valid @RequestBody AdminCancelOrderRequest request
    ) {
        return adminOrderService.cancelOrder(id, request);
    }
}