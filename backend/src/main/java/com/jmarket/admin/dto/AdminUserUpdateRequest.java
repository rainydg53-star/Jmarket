package com.jmarket.admin.dto;

import java.time.LocalDateTime;

public record AdminUserUpdateRequest(
        String name,
        String nickname,
        String phoneNumber,
        String role,
        Boolean banned,
        LocalDateTime bannedUntil,
        String banReason
) {
}
