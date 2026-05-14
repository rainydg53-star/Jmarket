package com.jmarket.admin.dto;

import com.jmarket.admin.domain.UserRestriction;
import java.time.LocalDateTime;

public record AdminRestrictionResponse(
        Long id,
        Long userId,
        String userNickname,
        String type,
        String typeLabel,
        String reason,
        LocalDateTime restrictedUntil,
        boolean active,
        LocalDateTime createdAt
) {
    public static AdminRestrictionResponse from(UserRestriction restriction) {
        return new AdminRestrictionResponse(
                restriction.getId(),
                restriction.getUser().getId(),
                restriction.getUser().getNickname(),
                restriction.getType().name(),
                restriction.getType().label(),
                restriction.getReason(),
                restriction.getRestrictedUntil(),
                restriction.isCurrentlyActive(),
                restriction.getCreatedAt()
        );
    }
}
