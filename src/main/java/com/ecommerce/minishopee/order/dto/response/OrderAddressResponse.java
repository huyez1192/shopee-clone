package com.ecommerce.minishopee.order.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OrderAddressResponse {

    private String recipientName;

    private String recipientPhone;

    private String province;

    private String ward;

    private String detailAddress;

    private String note;
}