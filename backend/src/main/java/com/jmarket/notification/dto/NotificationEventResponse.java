package com.jmarket.notification.dto;

import java.time.Instant;

public record NotificationEventResponse(
        Long notificationId,
        NotificationType type,
        String title,
        String message,
        String link,
        Instant occurredAt,
        Instant readAt
) {
}
