package com.ecommerce.minishopee.address.controller;

import com.ecommerce.minishopee.address.dto.request.AddressRequest;
import com.ecommerce.minishopee.address.dto.response.AddressResponse;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.address.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {
    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public List<AddressResponse> getMyAddresses(@AuthenticationPrincipal AppUser user) {
        return addressService.getMyAddresses(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse createAddress(@AuthenticationPrincipal AppUser user, @Valid @RequestBody AddressRequest request) {
        return addressService.createAddress(user, request);
    }

    @PutMapping("/{id}")
    public AddressResponse updateAddress(@PathVariable Long id, @AuthenticationPrincipal AppUser user, @Valid @RequestBody AddressRequest request) {
        return addressService.updateAddress(id, user, request);
    }

    @PatchMapping("/{id}/default")
    public AddressResponse setDefaultAddress(@PathVariable Long id, @AuthenticationPrincipal AppUser user) {
        return addressService.setDefaultAddress(id, user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(@PathVariable Long id, @AuthenticationPrincipal AppUser user) {
        addressService.deleteAddress(id, user);
    }
}
