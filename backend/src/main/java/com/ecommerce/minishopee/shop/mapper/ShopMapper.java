package com.ecommerce.minishopee.shop.mapper;

import com.ecommerce.minishopee.shop.dto.request.ShopRegisterRequest;
import com.ecommerce.minishopee.shop.dto.response.ShopResponse;
import com.ecommerce.minishopee.shop.dto.request.ShopUpdateRequest;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.shop.entity.Shop;

public class ShopMapper {

    public static Shop toEntity(ShopRegisterRequest request, AppUser owner) {

        Shop shop = new Shop();

        shop.setOwner(owner);
        shop.setName(request.getName().trim());
        shop.setDescription(request.getDescription());
        shop.setLogoUrl(request.getLogoUrl());
        shop.setAddress(request.getAddress().trim());
        shop.setContactPhone(request.getContactPhone().trim());

        return shop;
    }

    public static ShopResponse toResponse(Shop shop) {

        return ShopResponse.builder()
                .id(shop.getId())
                .ownerId(shop.getOwner().getId())
                .ownerEmail(shop.getOwner().getEmail())
                .name(shop.getName())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .address(shop.getAddress())
                .contactPhone(shop.getContactPhone())
                .status(shop.getStatus())
                .rejectionReason(shop.getRejectionReason())
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .build();
    }

    public static void updateEntity(Shop shop, ShopUpdateRequest request) {

        shop.setName(request.getName().trim());
        shop.setDescription(request.getDescription());
        shop.setLogoUrl(request.getLogoUrl());
        shop.setAddress(request.getAddress().trim());
        shop.setContactPhone(request.getContactPhone().trim());
    }
}