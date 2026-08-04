package com.ecommerce.minishopee.dto;

import com.ecommerce.minishopee.entity.OrderCancelledBy;
import com.ecommerce.minishopee.entity.OrderStatus;
import com.ecommerce.minishopee.entity.PaymentMethod;
import com.ecommerce.minishopee.entity.PaymentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class OrderResponse {

    private Long id;

    private String orderCode;

    private Long buyerId;

    private Long shopId;

    private String shopName;

    private OrderStatus status;

    private PaymentMethod paymentMethod;

    private PaymentStatus paymentStatus;

    private OrderAddressResponse address;

    private List<OrderItemResponse> items;

    private Integer itemCount;

    private Integer totalQuantity;

    private BigDecimal merchandiseSubtotal;

    private BigDecimal shippingFee;

    private BigDecimal totalAmount;


    // Các trường này null nếu đơn chưa bị hủy.
    private OrderCancelledBy cancelledBy;

    private String cancelReason;

    private LocalDateTime cancelledAt;



    private LocalDateTime confirmedAt;

    private LocalDateTime shippedAt;

    private LocalDateTime completedAt;


    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}