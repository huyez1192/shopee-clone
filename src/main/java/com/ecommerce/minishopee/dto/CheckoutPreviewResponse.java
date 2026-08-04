package com.ecommerce.minishopee.dto;

import com.ecommerce.minishopee.entity.PaymentMethod;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class CheckoutPreviewResponse {

    private CheckoutAddressResponse address;

    private List<CheckoutShopGroupResponse> shopGroups;

    private Integer shopCount;

    private Integer itemCount;

    private Integer totalQuantity;

    private BigDecimal merchandiseTotal;

    private BigDecimal shippingTotal;

    private BigDecimal grandTotal;

    private PaymentMethod paymentMethod;
}