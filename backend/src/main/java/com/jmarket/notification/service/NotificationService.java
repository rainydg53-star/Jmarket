package com.jmarket.notification.service;

import com.jmarket.auth.domain.User;
import com.jmarket.auth.repository.UserRepository;
import com.jmarket.common.exception.ErrorCode;
import com.jmarket.common.exception.JmarketException;
import com.jmarket.notification.domain.Notification;
import com.jmarket.notification.dto.NotificationReadAllResponse;
import com.jmarket.notification.dto.NotificationResponse;
import com.jmarket.notification.dto.NotificationType;
import com.jmarket.notification.dto.NotificationUnreadCountResponse;
import com.jmarket.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Notification create(
            Long recipientUserId,
            NotificationType type,
            String title,
            String message,
            String link
    ) {
        Notification notification = new Notification(
                recipientUserId,
                type,
                title,
                message,
                link,
                Instant.now()
        );
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(String currentUserEmail, boolean unreadOnly) {
        Long userId = findUserByEmail(currentUserEmail).getId();
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findAllByRecipientUserIdAndReadAtIsNullOrderByIdDesc(userId)
                : notificationRepository.findAllByRecipientUserIdOrderByIdDesc(userId);
        return notifications.stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public NotificationUnreadCountResponse getUnreadCount(String currentUserEmail) {
        Long userId = findUserByEmail(currentUserEmail).getId();
        long unreadCount = notificationRepository.countByRecipientUserIdAndReadAtIsNull(userId);
        return new NotificationUnreadCountResponse(unreadCount);
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId, String currentUserEmail) {
        Long userId = findUserByEmail(currentUserEmail).getId();
        Notification notification = notificationRepository.findByIdAndRecipientUserId(notificationId, userId)
                .orElseThrow(() -> new JmarketException(ErrorCode.NOTIFICATION_NOT_FOUND));
        notification.markRead();
        return NotificationResponse.from(notification);
    }

    @Transactional
    public NotificationReadAllResponse markAllRead(String currentUserEmail) {
        Long userId = findUserByEmail(currentUserEmail).getId();
        int updated = notificationRepository.markAllRead(userId, Instant.now());
        return new NotificationReadAllResponse(updated);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
    }
}
