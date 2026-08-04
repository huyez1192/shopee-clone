package com.ecommerce.minishopee.order.checkout.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class CheckoutItemResponse {

    private Long cartItemId;

    private Long variantId;

    private Long productId;

    private String productName;

    private String productImageUrl;

    private String variantName1;

    private String variantValue1;

    private String variantName2;

    private String variantValue2;

    private BigDecimal unitPrice;

    private Integer quantity;

    private Integer availableStock;

    private BigDecimal lineTotal;
}