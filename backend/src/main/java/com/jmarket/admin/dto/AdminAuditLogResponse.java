package com.jmarket.admin.dto;

import com.jmarket.admin.domain.AdminAuditLog;
import java.time.LocalDateTime;

public record AdminAuditLogResponse(
        Long id,
        Long adminUserId,
        String adminNickname,
        String action,
        String targetType,
        Long targetId,
        String memo,
        LocalDateTime createdAt
) {
    public static AdminAuditLogResponse from(AdminAuditLog log) {
        return new AdminAuditLogResponse(
                log.getId(),
                log.getAdminUser().getId(),
                log.getAdminUser().getNickname(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getMemo(),
                log.getCreatedAt()
        );
    }
}
