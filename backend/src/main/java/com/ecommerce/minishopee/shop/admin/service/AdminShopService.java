package com.ecommerce.minishopee.shop.admin.service;

import com.ecommerce.minishopee.shop.admin.dto.request.RejectShopRequest;
import com.ecommerce.minishopee.shop.dto.response.ShopResponse;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.shop.enums.ShopStatus;
import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.shop.exception.ShopNotFoundException;
import com.ecommerce.minishopee.shop.mapper.ShopMapper;
import com.ecommerce.minishopee.user.repository.AppUserRepository;
import com.ecommerce.minishopee.shop.repository.ShopRepository;
import com.ecommerce.minishopee.user.enums.Role;
import com.ecommerce.minishopee.notification.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminShopService {

    private final ShopRepository shopRepository;
    private final AppUserRepository appUserRepository;
    private final NotificationService notificationService;

    public AdminShopService(
            ShopRepository shopRepository,
            AppUserRepository appUserRepository,
            NotificationService notificationService
    ) {
        this.shopRepository = shopRepository;
        this.appUserRepository = appUserRepository;
        this.notificationService = notificationService;
    }

    // admin xem tất cả shop
    public List<ShopResponse> getAllShops() {

        return shopRepository.findAll()
                .stream()
                .map(ShopMapper :: toResponse)
                .toList();
    }

    // admin xem chi tiết 1 shop
    public ShopResponse getShopById(Long id) {
        Shop shop = findShop(id);

        return ShopMapper.toResponse(shop);
    }


    /*
     * Admin duyệt shop.
     *
     * Hai thay đổi phải thành công cùng nhau:
     * Shop: PENDING -> APPROVED
     * User: USER -> SELLER
     */
    @Transactional
    public ShopResponse approveShop(Long id) {
        Shop shop = findShop(id);

        if (shop.getStatus() != ShopStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể duyệt shop ở trạng thái PENDING.");
        }

        AppUser owner = shop.getOwner();

        shop.setStatus(ShopStatus.APPROVED);
        shop.setRejectionReason(null);   // Xóa lý do từ chối cũ nếu có
        owner.setRole(Role.SELLER);

        appUserRepository.save(owner);
        Shop updatedshop = shopRepository.save(shop);

        notificationService.notifyShopApproved(updatedshop);

        return ShopMapper.toResponse(updatedshop);
    }


    // admin từ chối shop
    @Transactional
    public ShopResponse rejectShop(Long id, RejectShopRequest request) {
        Shop shop = findShop(id);

        if (shop.getStatus() != ShopStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể từ chối shop ở trạng thái PENDING.");
        }

        shop.setStatus(ShopStatus.REJECTED);
        shop.setRejectionReason(request.getReason().trim());

        Shop updatedShop = shopRepository.save(shop);

        notificationService.notifyShopRejected(updatedShop);

        return ShopMapper.toResponse(updatedShop);
    }

    /*
     * Admin khóa shop.
     *
     * Chủ shop vẫn giữ role SELLER.
     * Nhưng shop bị LOCKED thì không được quản lý hoặc đăng sản phẩm mới.
     */
    @Transactional
    public ShopResponse lockShop(Long id) {
        Shop shop = findShop(id);

        if (shop.getStatus() != ShopStatus.APPROVED) {
            throw new BadRequestException("Chỉ có thể khóa shop đang ở trạng thái APPROVED.");
        }

        shop.setStatus(ShopStatus.LOCKED);

        Shop updatedShop = shopRepository.save(shop);

        return ShopMapper.toResponse(updatedShop);
    }


    /*
     * Mở khóa shop:
     *
     * LOCKED -> APPROVED
     */
    @Transactional
    public ShopResponse unlockShop(Long id) {
        Shop shop = findShop(id);

        if (shop.getStatus() != ShopStatus.LOCKED) {
            throw new BadRequestException("Chỉ có thể mở khóa shop ở trạng thái LOCKED.");
        }

        shop.setStatus(ShopStatus.APPROVED);

        Shop updatedShop = shopRepository.save(shop);

        return ShopMapper.toResponse(updatedShop);
    }


    private Shop findShop(Long id) {
        return shopRepository.findById(id)
                .orElseThrow(() -> new ShopNotFoundException(id));
    }
}
