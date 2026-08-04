package com.ecommerce.minishopee.cart.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class CartResponse {

    private Long cartId;

    private Long userId;

    private List<CartItemResponse> items;

    // số item trong 1 cart
    private Integer itemCount;

    // tổng số item
    private Integer totalQuantity;

    // tổng các item còn hợp lệ
    private BigDecimal totalAmount;
}
