package com.ecommerce.minishopee.mapper;

import com.ecommerce.minishopee.dto.AddressRequest;
import com.ecommerce.minishopee.dto.AddressResponse;
import com.ecommerce.minishopee.entity.Address;
import com.ecommerce.minishopee.entity.AppUser;

public class AddressMapper {

    public static Address toEntity(AddressRequest request, AppUser user) {
        Address address = new Address();

        address.setUser(user);
        address.setRecipientName(request.getRecipientName().trim());
        address.setRecipientPhone(request.getRecipientPhone().trim());
        address.setProvince(request.getProvince().trim());
        address.setWard(request.getWard().trim());
        address.setDetailAddress(request.getDetailAddress().trim());
        address.setNote(request.getNote());
        address.setDefaultAddress(request.isDefaultAddress());

        return address;
    }

    public static AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
                .province(address.getProvince())
                .ward(address.getWard())
                .detailAddress(address.getDetailAddress())
                .note(address.getNote())
                .defaultAddress(address.isDefaultAddress())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }

    public static void updateEntity(Address address, AddressRequest request) {
        address.setRecipientName(request.getRecipientName().trim());
        address.setRecipientPhone(request.getRecipientPhone().trim());
        address.setProvince(request.getProvince().trim());
        address.setWard(request.getWard().trim());
        address.setDetailAddress(request.getDetailAddress().trim());
        address.setNote(request.getNote());
    }

}
