package com.jmarket.admin.dto;

import java.time.LocalDateTime;

public record AdminUserBanRequest(
        LocalDateTime bannedUntil,
        String reason
) {
}
