package com.ecommerce.minishopee.shop.admin.service;

import com.ecommerce.minishopee.common.exception.BadRequestException;
import com.ecommerce.minishopee.notification.service.NotificationService;
import com.ecommerce.minishopee.shop.admin.dto.request.RejectShopRequest;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.shop.enums.ShopStatus;
import com.ecommerce.minishopee.shop.repository.ShopRepository;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.user.enums.Role;
import com.ecommerce.minishopee.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminShopNotificationTests {
    @Mock ShopRepository shopRepository;
    @Mock AppUserRepository userRepository;
    @Mock NotificationService notificationService;
    AdminShopService service;
    Shop shop;

    @BeforeEach
    void setUp() {
        service = new AdminShopService(shopRepository, userRepository, notificationService);
        AppUser owner = new AppUser();
        owner.setRole(Role.USER);
        shop = new Shop();
        shop.setId(1L);
        shop.setOwner(owner);
        shop.setStatus(ShopStatus.PENDING);
        when(shopRepository.findById(1L)).thenReturn(Optional.of(shop));
        when(shopRepository.save(shop)).thenReturn(shop);
    }

    @Test
    void approveNotifiesOwnerOnlyAfterValidTransition() {
        service.approveShop(1L);
        verify(notificationService).notifyShopApproved(shop);
        shop.setStatus(ShopStatus.APPROVED);
        assertThrows(BadRequestException.class, () -> service.approveShop(1L));
        verify(notificationService, times(1)).notifyShopApproved(shop);
    }

    @Test
    void rejectNotifiesOwnerOnlyAfterValidTransition() {
        RejectShopRequest request = new RejectShopRequest();
        request.setReason("Missing information");
        service.rejectShop(1L, request);
        verify(notificationService).notifyShopRejected(shop);
        shop.setStatus(ShopStatus.REJECTED);
        assertThrows(BadRequestException.class, () -> service.rejectShop(1L, request));
        verify(notificationService, times(1)).notifyShopRejected(shop);
    }
}
