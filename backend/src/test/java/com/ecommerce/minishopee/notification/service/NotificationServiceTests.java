package com.ecommerce.minishopee.notification.service;

import com.ecommerce.minishopee.notification.entity.Notification;
import com.ecommerce.minishopee.notification.enums.NotificationType;
import com.ecommerce.minishopee.notification.exception.NotificationNotFoundException;
import com.ecommerce.minishopee.notification.repository.NotificationRepository;
import com.ecommerce.minishopee.user.entity.AppUser;
import com.ecommerce.minishopee.order.entity.Order;
import com.ecommerce.minishopee.shop.entity.Shop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTests {
    @Mock NotificationRepository repository;
    NotificationService service;
    AppUser user;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository);
        user = new AppUser();
        user.setId(1L);
    }

    @Test
    void listQueriesOnlyCurrentUsersNotifications() {
        when(repository.findByRecipient_Id(eq(1L), any())).thenReturn(new PageImpl<>(List.of()));
        service.getNotifications(user, null, 0, 20);
        verify(repository).findByRecipient_Id(eq(1L), any());
        verify(repository, never()).findAll(any(Pageable.class));
    }

    @Test
    void cannotMarkAnotherUsersNotification() {
        when(repository.findByIdAndRecipient_Id(9L, 1L)).thenReturn(Optional.empty());
        assertThrows(NotificationNotFoundException.class, () -> service.markAsRead(9L, user));
        verify(repository, never()).save(any());
    }

    @Test
    void markUnreadSetsReadAndTimestamp() {
        Notification notification = notification(false);
        when(repository.findByIdAndRecipient_Id(2L, 1L)).thenReturn(Optional.of(notification));
        service.markAsRead(2L, user);
        assertTrue(notification.isRead());
        assertNotNull(notification.getReadAt());
        verify(repository).save(notification);
    }

    @Test
    void markingReadAgainKeepsFirstReadTime() {
        Notification notification = notification(true);
        LocalDateTime firstReadAt = LocalDateTime.now().minusHours(1);
        notification.setReadAt(firstReadAt);
        when(repository.findByIdAndRecipient_Id(2L, 1L)).thenReturn(Optional.of(notification));
        service.markAsRead(2L, user);
        assertEquals(firstReadAt, notification.getReadAt());
        verify(repository, never()).save(any());
    }

    @Test
    void markAllUsesSingleOwnerScopedBulkUpdate() {
        when(repository.markAllRead(eq(1L), any())).thenReturn(3);
        assertEquals(3, service.markAllAsRead(user).updatedCount());
        verify(repository).markAllRead(eq(1L), any());
    }

    @Test
    void unreadCountUsesDatabaseCount() {
        when(repository.countByRecipient_IdAndReadFalse(1L)).thenReturn(5L);
        assertEquals(5L, service.getUnreadCount(user).count());
    }

    @Test
    void paginationRequestsNewestFirst() {
        when(repository.findByRecipient_Id(eq(1L), any())).thenReturn(new PageImpl<>(List.of()));
        service.getNotifications(user, null, 2, 10);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByRecipient_Id(eq(1L), captor.capture());
        Pageable pageable = captor.getValue();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertTrue(pageable.getSort().getOrderFor("createdAt").isDescending());
    }

    @Test
    void orderCreationNotifiesBuyerAndCorrectShopOwner() {
        AppUser seller = new AppUser(); seller.setId(2L);
        Order order = order(10L, user, seller);
        service.notifyOrderCreated(order);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(captor.capture());
        List<Notification> saved = captor.getAllValues();
        assertEquals(user, saved.get(0).getRecipient());
        assertEquals(NotificationType.ORDER_CREATED, saved.get(0).getType());
        assertEquals(seller, saved.get(1).getRecipient());
        assertEquals(NotificationType.NEW_ORDER, saved.get(1).getType());
        assertEquals("/seller/orders/10", saved.get(1).getTargetUrl());
    }

    @Test
    void multiShopOrderCreationKeepsRecipientsSeparatedPerOrder() {
        AppUser sellerA = new AppUser(); sellerA.setId(2L);
        AppUser sellerB = new AppUser(); sellerB.setId(3L);
        AppUser sellerC = new AppUser(); sellerC.setId(4L);
        service.notifyOrderCreated(order(10L, user, sellerA));
        service.notifyOrderCreated(order(11L, user, sellerB));
        service.notifyOrderCreated(order(12L, user, sellerC));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(6)).save(captor.capture());
        List<Notification> saved = captor.getAllValues();
        assertEquals(3, saved.stream().filter(n -> n.getRecipient() == user).count());
        assertEquals(1, saved.stream().filter(n -> n.getRecipient() == sellerA).count());
        assertEquals(1, saved.stream().filter(n -> n.getRecipient() == sellerB).count());
        assertEquals(1, saved.stream().filter(n -> n.getRecipient() == sellerC).count());
    }

    private Notification notification(boolean read) {
        Notification notification = new Notification();
        notification.setId(2L);
        notification.setRecipient(user);
        notification.setType(NotificationType.ORDER_CREATED);
        notification.setTitle("title");
        notification.setMessage("message");
        notification.setTargetUrl("/orders/1");
        notification.setRead(read);
        notification.setCreatedAt(LocalDateTime.now());
        return notification;
    }

    private Order order(Long id, AppUser buyer, AppUser seller) {
        Shop shop = new Shop();
        shop.setOwner(seller);
        Order order = new Order();
        order.setId(id);
        order.setOrderCode("ORDER-" + id);
        order.setBuyer(buyer);
        order.setShop(shop);
        return order;
    }
}
