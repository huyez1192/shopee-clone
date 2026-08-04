package com.ecommerce.minishopee.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AddressResponse {
    private Long id;

    private String recipientName;

    private String recipientPhone;

    private String province;

    private String ward;

    private String detailAddress;

    private String note;

    private boolean defaultAddress;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
