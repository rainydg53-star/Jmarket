package com.jmarket.admin.dto;

public record AdminAuditActionMetricResponse(
        String action,
        long count
) {
}
