package com.ecommerce.minishopee.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ecommerce.minishopee.notification.enums.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private String targetUrl;
    @JsonProperty("isRead")
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
