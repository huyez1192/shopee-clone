package com.ecommerce.minishopee.address.service;

import com.ecommerce.minishopee.address.dto.request.AddressRequest;
import com.ecommerce.minishopee.address.dto.response.AddressResponse;
import com.ecommerce.minishopee.address.entity.Address;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.address.exception.AddressNotFoundException;
import com.ecommerce.minishopee.address.mapper.AddressMapper;
import com.ecommerce.minishopee.address.repository.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public List<AddressResponse> getMyAddresses(AppUser currentUser) {
        return addressRepository
                .findAllByUserIdOrderByDefaultAddressDescCreatedAtDesc(currentUser.getId())
                .stream()
                .map(AddressMapper::toResponse)
                .toList();
    }

    @Transactional
    public AddressResponse createAddress(AppUser currentUser, AddressRequest request) {

        boolean firstAddress = !addressRepository.existsByUserId(currentUser.getId());

        /*
         * Địa chỉ đầu tiên tự động trở thành mặc định.
         *
         * Hoặc nếu request.defaultAddress = true thì bỏ mặc định của các địa chỉ cũ.
         */
        if (firstAddress || request.isDefaultAddress()) {
            removeCurrentDefaultAddress(currentUser.getId());
        }

        Address address = AddressMapper.toEntity(request, currentUser);

        if (firstAddress) {
            address.setDefaultAddress(true);
        }

        Address savedAddress = addressRepository.save(address);

        return AddressMapper.toResponse(savedAddress);
    }

    @Transactional
    public AddressResponse updateAddress(Long id, AppUser currentUser, AddressRequest request) {

        Address address = findUserAddress(id, currentUser.getId());

        AddressMapper.updateEntity(address, request);

        if (request.isDefaultAddress()) {

            removeCurrentDefaultAddress(currentUser.getId());

            address.setDefaultAddress(true);
        }

        Address updatedAddress = addressRepository.save(address);

        return AddressMapper.toResponse(updatedAddress);
    }

    @Transactional
    public AddressResponse setDefaultAddress(Long id, AppUser currentUser) {

        Address address = findUserAddress(id, currentUser.getId());

        removeCurrentDefaultAddress(currentUser.getId());

        address.setDefaultAddress(true);

        Address updatedAddress = addressRepository.save(address);

        return AddressMapper.toResponse(updatedAddress);
    }

    @Transactional
    public void deleteAddress(Long id, AppUser currentUser) {

        Address address = findUserAddress(id, currentUser.getId());

        boolean wasDefault = address.isDefaultAddress();

        addressRepository.delete(address);

        // Nếu vừa xóa địa chỉ mặc định, lấy một địa chỉ còn lại làm mặc định.
        if (wasDefault) {
            List<Address> remainingAddresses =
                    addressRepository
                            .findAllByUserIdOrderByDefaultAddressDescCreatedAtDesc(
                                    currentUser.getId()
                            );

            if (!remainingAddresses.isEmpty()) {
                Address newDefault = remainingAddresses.get(0);

                newDefault.setDefaultAddress(true);

                addressRepository.save(newDefault);
            }
        }
    }

    private Address findUserAddress(Long addressId, Long userId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AddressNotFoundException(addressId));
    }

    private void removeCurrentDefaultAddress(Long userId) {

        List<Address> addresses =
                addressRepository
                        .findAllByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId);

        for (Address address : addresses) {

            if (address.isDefaultAddress()) {
                address.setDefaultAddress(false);
            }
        }

        addressRepository.saveAll(addresses);
    }


    // quy tắc
    //    Địa chỉ đầu tiên
    //→ tự động default
    //
    //    Chọn Address B làm default
    //→ Address A tự động mất default
    //
    //    Xóa địa chỉ default
    //→ một địa chỉ còn lại tự động được chọn làm default
}
