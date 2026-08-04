package com.ecommerce.minishopee.address.repository;

import com.ecommerce.minishopee.address.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findAllByUserIdOrderByDefaultAddressDescCreatedAtDesc(Long userId);

    Optional<Address> findByIdAndUserId(Long id, Long userId);
    // cái này để USER A không thể sửa/xóa địa chỉ của USER B

    boolean existsByUserId(Long userId);
}
