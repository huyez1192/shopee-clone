package com.ecommerce.minishopee.dto;

import com.ecommerce.minishopee.entity.CartItemAvailability;
import com.ecommerce.minishopee.entity.ProductVariantStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class CartItemResponse {

    private Long id;

    private Long variantId;

    private Long productId;

    private String productName;

    private String productImageUrl;

    private Long shopId;

    private String shopName;

    private String variantName1;

    private String variantValue1;

    private String variantName2;

    private String variantValue2;

    private BigDecimal unitPrice;

    private Integer quantity;

    private Integer availableStock;

    private BigDecimal lineTotal;

    private ProductVariantStatus variantStatus;

    private boolean available;

    private CartItemAvailability availabilityStatus;

    private String unavailableReason;
}
