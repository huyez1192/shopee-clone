package com.ecommerce.minishopee.shop.service;

import com.ecommerce.minishopee.shop.dto.request.ShopRegisterRequest;
import com.ecommerce.minishopee.shop.dto.response.ShopResponse;
import com.ecommerce.minishopee.shop.dto.request.ShopUpdateRequest;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.shop.enums.ShopStatus;
import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.shop.exception.ShopNotFoundException;
import com.ecommerce.minishopee.shop.mapper.ShopMapper;
import com.ecommerce.minishopee.shop.repository.ShopRepository;
import org.springframework.stereotype.Service;

@Service
public class ShopService {

    private final ShopRepository shopRepository;

    public ShopService(ShopRepository shopRepository) {
        this.shopRepository = shopRepository;
    }

    /*
     * User gửi yêu cầu mở shop.
     *
     * Shop mới luôn ở trạng thái PENDING.
     * User chưa được đổi thành SELLER ở bước này.
     */
    public ShopResponse registerShop(AppUser currentUser, ShopRegisterRequest request) {

        /*
         * Mỗi user chỉ có một shop.
         */
        if (shopRepository.existsByOwnerId(currentUser.getId())) {
            throw new BadRequestException("Bạn đã đăng ký shop.");
        }

        String shopName = request.getName().trim();

        if (shopRepository.existsByNameIgnoreCase(shopName)) {
            throw new BadRequestException("Tên shop đã tồn tại.");
        }

        Shop shop = ShopMapper.toEntity(request, currentUser);

        /*
         * Backend tự quyết định status.
         * Client không thể tự tạo APPROVED shop.
         */
        shop.setStatus(ShopStatus.PENDING);

        Shop savedShop = shopRepository.save(shop);

        return ShopMapper.toResponse(savedShop);
    }

    /*
     * User/Seller xem shop của chính mình.
     */
    public ShopResponse getMyShop(AppUser currentUser) {

        Shop shop = shopRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ShopNotFoundException("Bạn chưa đăng ký shop."));

        return ShopMapper.toResponse(shop);
    }

    /*
     * Cập nhật thông tin shop.
     *
     * Chỉ owner của shop mới truy cập được,
     * vì shop được tìm theo currentUser.id.
     */
    public ShopResponse updateMyShop(AppUser currentUser, ShopUpdateRequest request) {

        Shop shop = shopRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ShopNotFoundException("Bạn chưa đăng ký shop."));

        String shopName = request.getName().trim();

        shopRepository
                .findAll()
                .stream()
                .filter(existing -> existing.getName().equalsIgnoreCase(shopName))
                .filter(existing -> !existing.getId().equals(shop.getId()))
                .findAny()
                .ifPresent(existing -> {throw new BadRequestException("Tên shop đã tồn tại.");
                });

        ShopMapper.updateEntity(shop, request);

        Shop updatedShop = shopRepository.save(shop);

        return ShopMapper.toResponse(updatedShop);
    }
}