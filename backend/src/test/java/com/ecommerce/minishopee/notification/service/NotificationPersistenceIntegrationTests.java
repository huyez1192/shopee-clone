package com.ecommerce.minishopee.notification.service;

import com.ecommerce.minishopee.notification.entity.Notification;
import com.ecommerce.minishopee.notification.enums.NotificationType;
import com.ecommerce.minishopee.notification.repository.NotificationRepository;
import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.product.entity.Product;
import com.ecommerce.minishopee.review.entity.Review;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.shop.enums.ShopStatus;
import com.ecommerce.minishopee.shop.repository.ShopRepository;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.user.enums.Role;
import com.ecommerce.minishopee.user.enums.UserStatus;
import com.ecommerce.minishopee.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationPersistenceIntegrationTests {
    @Autowired NotificationService notificationService;
    @Autowired NotificationRepository notificationRepository;
    @Autowired AppUserRepository userRepository;
    @Autowired ShopRepository shopRepository;

    @Test
    void representativeOrderCancelReviewAndApprovalNotificationsPersistCorrectPayloads() {
        String suffix = UUID.randomUUID().toString();
        AppUser buyer = user("buyer-" + suffix, Role.USER);
        AppUser seller = user("seller-" + suffix, Role.SELLER);
        Shop shop = new Shop(); shop.setOwner(seller); shop.setName("Shop " + suffix); shop.setAddress("Address");
        shop.setContactPhone("0123456789"); shop.setStatus(ShopStatus.APPROVED); shop = shopRepository.saveAndFlush(shop);
        Order order = new Order(); order.setId(12345L); order.setOrderCode("ORDER-12345"); order.setBuyer(buyer); order.setShop(shop);
        Product product = new Product(); product.setName("Reviewed product"); product.setShop(shop);
        Review review = new Review(); review.setProduct(product); review.setRating(5);

        notificationService.notifyOrderConfirmed(order);
        notificationService.notifyOrderCancelledToBuyer(order, false);
        notificationService.notifyProductReviewed(review);
        notificationService.notifyShopApproved(shop);
        notificationRepository.flush();

        List<Notification> buyerNotifications = notificationRepository.findAll().stream()
                .filter(notification -> notification.getRecipient().getId().equals(buyer.getId())).toList();
        List<Notification> sellerNotifications = notificationRepository.findAll().stream()
                .filter(notification -> notification.getRecipient().getId().equals(seller.getId())).toList();
        assertNotification(buyerNotifications, NotificationType.ORDER_CONFIRMED, "/orders/12345");
        assertNotification(buyerNotifications, NotificationType.ORDER_CANCELLED, "/orders/12345");
        assertNotification(sellerNotifications, NotificationType.PRODUCT_REVIEWED, "/seller/reviews");
        assertNotification(sellerNotifications, NotificationType.SHOP_APPROVED, "/seller");
    }

    private void assertNotification(List<Notification> notifications, NotificationType type, String targetUrl) {
        Notification notification = notifications.stream().filter(item -> item.getType() == type).findFirst().orElseThrow();
        assertEquals(targetUrl, notification.getTargetUrl());
        assertFalse(notification.isRead());
        assertFalse(notification.getTitle().isBlank());
        assertFalse(notification.getMessage().isBlank());
    }

    private AppUser user(String value, Role role) {
        AppUser user = new AppUser(); user.setFullName(value); user.setEmail(value + "@test.local");
        user.setPassword("password"); user.setPhone(value); user.setRole(role); user.setStatus(UserStatus.ACTIVE);
        return userRepository.saveAndFlush(user);
    }
}
