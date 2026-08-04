package com.ecommerce.minishopee.controller;

import com.ecommerce.minishopee.dto.RejectShopRequest;
import com.ecommerce.minishopee.dto.ShopResponse;
import com.ecommerce.minishopee.service.AdminShopService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/shops")
public class AdminShopController {

    private final AdminShopService adminShopService;

    public AdminShopController(AdminShopService adminShopService) {
        this.adminShopService = adminShopService;
    }

    //  Xem tất cả shop.
    @GetMapping
    public List<ShopResponse> getAllShops() {
        return adminShopService.getAllShops();
    }

    // Xem chi tiết shop.
    @GetMapping("/{id}")
    public ShopResponse getShopById (@PathVariable Long id) {
        return adminShopService.getShopById(id);
    }

    // Duyệt shop.
    @PatchMapping("/{id}/approve")
    public ShopResponse approveShop(@PathVariable Long id) {
        return adminShopService.approveShop(id);
    }


    // Từ chối shop.
    @PatchMapping("/{id}/reject")
    public ShopResponse rejectShop(@PathVariable Long id, @Valid @RequestBody RejectShopRequest request) {
        return adminShopService.rejectShop(id, request);
    }

    // Khóa shop.
    @PatchMapping("/{id}/lock")
    public ShopResponse lockShop(@PathVariable Long id) {
        return adminShopService.lockShop(id);
    }


    // Mở khóa shop.
    @PatchMapping("/{id}/unlock")
    public ShopResponse unlockShop(@PathVariable Long id) {
        return adminShopService.unlockShop(id);
    }
}
