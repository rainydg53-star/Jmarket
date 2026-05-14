package com.jmarket.notification.dto;

import com.jmarket.notification.domain.Notification;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        String link,
        Instant occurredAt,
        Instant createdAt,
        Instant readAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getLink(),
                notification.getOccurredAt(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
