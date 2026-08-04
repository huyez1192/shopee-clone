package com.ecommerce.minishopee.order.checkout.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class CheckoutShopGroupResponse {

    private Long shopId;

    private String shopName;

    private List<CheckoutItemResponse> items;

    private Integer itemCount;

    private Integer totalQuantity;

    private BigDecimal merchandiseSubtotal;

    private BigDecimal shippingFee;

    private BigDecimal shopTotal;
}