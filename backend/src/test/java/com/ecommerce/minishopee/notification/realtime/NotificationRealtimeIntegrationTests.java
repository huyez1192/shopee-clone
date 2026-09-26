package com.ecommerce.minishopee.notification.realtime;

import com.ecommerce.minishopee.notification.dto.response.NotificationResponse;
import com.ecommerce.minishopee.notification.enums.NotificationType;
import com.ecommerce.minishopee.notification.repository.NotificationRepository;
import com.ecommerce.minishopee.notification.service.NotificationService;
import com.ecommerce.minishopee.shop.entity.Shop;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.user.enums.Role;
import com.ecommerce.minishopee.user.enums.UserStatus;
import com.ecommerce.minishopee.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class NotificationRealtimeIntegrationTests {
    @Autowired NotificationService notificationService;
    @Autowired NotificationRepository notificationRepository;
    @Autowired AppUserRepository userRepository;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void resetMessaging() {
        reset(messagingTemplate);
    }

    @Test
    void representativeBusinessNotificationIsSentOnlyToRecipientAfterCommit() {
        AppUser recipient = persistUser("recipient");
        AppUser otherUser = persistUser("other");
        Shop shop = new Shop();
        shop.setName("Realtime Shop");
        shop.setOwner(recipient);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            notificationService.notifyShopApproved(shop);
            verifyNoInteractions(messagingTemplate);
        });

        verify(messagingTemplate).convertAndSendToUser(
                eq(recipient.getUsername()),
                eq(NotificationRealtimeListener.NOTIFICATIONS_DESTINATION),
                argThat(payload -> validCreatedPayload(payload, recipient.getId()))
        );
        verify(messagingTemplate, never()).convertAndSendToUser(eq(otherUser.getUsername()), anyString(), any());
    }

    @Test
    void businessTransactionRollbackPersistsAndSendsNothing() {
        AppUser recipient = persistUser("rollback");
        Shop shop = new Shop();
        shop.setName("Rollback Shop");
        shop.setOwner(recipient);

        assertThrows(IllegalStateException.class, () ->
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    notificationService.notifyShopApproved(shop);
                    verifyNoInteractions(messagingTemplate);
                    throw new IllegalStateException("force rollback");
                }));

        assertEquals(0L, notificationRepository.countByRecipient_IdAndReadFalse(recipient.getId()));
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void outboundContractsDoNotContainRecipientOrPrivateAccountData() {
        var eventFields = Arrays.stream(NotificationRealtimeEventResponse.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName).toList();
        var notificationFields = Arrays.stream(NotificationResponse.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName).toList();
        assertFalse(eventFields.contains("recipient") || eventFields.contains("recipientId"));
        assertFalse(notificationFields.contains("recipient") || notificationFields.contains("password")
                || notificationFields.contains("phone") || notificationFields.contains("email"));
    }

    private boolean validCreatedPayload(Object payload, Long recipientId) {
        if (!(payload instanceof NotificationRealtimeEventResponse event)
                || event.type() != NotificationRealtimeEventType.NOTIFICATION_CREATED) return false;
        NotificationResponse notification = event.notification();
        return notification != null
                && notification.getId() != null
                && notification.getType() == NotificationType.SHOP_APPROVED
                && !notification.getTitle().isBlank()
                && !notification.getMessage().isBlank()
                && "/seller".equals(notification.getTargetUrl())
                && !notification.isRead()
                && notification.getCreatedAt() != null
                && notificationRepository.findById(notification.getId())
                    .map(saved -> saved.getRecipient().getId().equals(recipientId)).orElse(false);
    }

    private AppUser persistUser(String prefix) {
        String unique = prefix + "-" + UUID.randomUUID();
        AppUser user = new AppUser();
        user.setFullName(unique);
        user.setEmail(unique + "@test.local");
        user.setPassword("password");
        user.setPhone(unique);
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.saveAndFlush(user);
    }
}
