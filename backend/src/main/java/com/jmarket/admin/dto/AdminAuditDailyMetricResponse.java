package com.jmarket.admin.dto;

import java.time.LocalDate;

public record AdminAuditDailyMetricResponse(
        LocalDate date,
        long actionCount
) {
}
