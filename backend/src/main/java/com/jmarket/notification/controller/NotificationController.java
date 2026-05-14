package com.jmarket.notification.controller;

import com.jmarket.auth.repository.UserRepository;
import com.jmarket.common.exception.ErrorCode;
import com.jmarket.common.exception.JmarketException;
import com.jmarket.notification.dto.NotificationReadAllResponse;
import com.jmarket.notification.dto.NotificationResponse;
import com.jmarket.notification.dto.NotificationUnreadCountResponse;
import com.jmarket.notification.service.NotificationService;
import com.jmarket.notification.service.NotificationSseService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationSseService notificationSseService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(
            NotificationSseService notificationSseService,
            NotificationService notificationService,
            UserRepository userRepository
    ) {
        this.notificationSseService = notificationSseService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal(expression = "username") String email) {
        if (email == null || email.isBlank()) {
            throw new JmarketException(ErrorCode.UNAUTHORIZED);
        }
        Long userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND))
                .getId();
        return notificationSseService.subscribe(userId);
    }

    @GetMapping("/me")
    public List<NotificationResponse> getMyNotifications(
            @AuthenticationPrincipal(expression = "username") String email,
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly
    ) {
        return notificationService.getMyNotifications(email, unreadOnly);
    }

    @GetMapping("/me/unread-count")
    public NotificationUnreadCountResponse getMyUnreadCount(
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return notificationService.getUnreadCount(email);
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return notificationService.markRead(notificationId, email);
    }

    @PatchMapping("/read-all")
    public NotificationReadAllResponse markAllRead(
            @AuthenticationPrincipal(expression = "username") String email
    ) {
        return notificationService.markAllRead(email);
    }
}
