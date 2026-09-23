package com.ecommerce.minishopee.notification.service;

import com.ecommerce.minishopee.notification.repository.NotificationRepository;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.user.enums.Role;
import com.ecommerce.minishopee.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class NotificationTransactionTests {
    @Autowired NotificationService notificationService;
    @Autowired NotificationRepository notificationRepository;
    @Autowired AppUserRepository userRepository;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void notificationRollsBackWithOwningBusinessTransaction() {
        AppUser owner = new AppUser();
        owner.setFullName("Transaction Test Owner");
        owner.setEmail("notification-rollback@example.test");
        owner.setPassword("test-password");
        owner.setPhone("0000000000");
        owner.setRole(Role.USER);
        owner = userRepository.saveAndFlush(owner);

        Shop shop = new Shop();
        shop.setName("Rollback Shop");
        shop.setOwner(owner);

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            notificationService.notifyShopApproved(shop);
            throw new IllegalStateException("force rollback");
        }));

        assertEquals(0L, notificationRepository.countByRecipient_IdAndReadFalse(owner.getId()));
    }
}
