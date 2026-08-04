package com.ecommerce.minishopee.dto;

import com.ecommerce.minishopee.entity.ShopStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ShopResponse {

    private Long id;

    private Long ownerId;

    private String ownerEmail;

    private String name;

    private String description;

    private String logoUrl;

    private String address;

    private String contactPhone;

    private ShopStatus status;

    private String rejectionReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}