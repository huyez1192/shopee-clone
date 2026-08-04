package com.ecommerce.minishopee.repository;

import com.ecommerce.minishopee.entity.Shop;
import com.ecommerce.minishopee.entity.ShopStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    boolean existsByOwnerId(Long ownerId);

    boolean existsByNameIgnoreCase(String name);

    Optional<Shop> findByNameIgnoreCase(String name);

    Optional<Shop> findByOwnerId(Long ownerId);

    Optional<Shop> findByIdAndStatus(Long id, ShopStatus status);

    List<Shop> findAllByStatus(ShopStatus status);

    List<Shop> findAllByOwner_IdIn(Collection<Long> ownerIds);
}