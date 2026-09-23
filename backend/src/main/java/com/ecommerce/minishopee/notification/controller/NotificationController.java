package com.ecommerce.minishopee.notification.controller;

import com.ecommerce.minishopee.common.dto.PageResponse;
import com.ecommerce.minishopee.notification.dto.response.MarkAllReadResponse;
import com.ecommerce.minishopee.notification.dto.response.NotificationResponse;
import com.ecommerce.minishopee.notification.dto.response.UnreadCountResponse;
import com.ecommerce.minishopee.notification.service.NotificationService;
import com.ecommerce.minishopee.user.entity.AppUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(
            @AuthenticationPrincipal AppUser currentUser,
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return notificationService.getNotifications(currentUser, read, page, size);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal AppUser currentUser) {
        return notificationService.getUnreadCount(currentUser);
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        return notificationService.markAsRead(id, currentUser);
    }

    @PatchMapping("/read-all")
    public MarkAllReadResponse markAllRead(@AuthenticationPrincipal AppUser currentUser) {
        return notificationService.markAllAsRead(currentUser);
    }
}
