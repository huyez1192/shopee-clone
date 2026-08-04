package com.ecommerce.minishopee.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class OrderItemResponse {

    private Long id;

    private Long productId;

    private Long variantId;

    private String productName;

    private String productImageUrl;

    private String skuCode;

    private String variantName1;

    private String variantValue1;

    private String variantName2;

    private String variantValue2;

    private BigDecimal unitPrice;

    private Integer quantity;

    private BigDecimal lineTotal;
}